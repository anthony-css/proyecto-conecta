package com.cibertec.conecta.service;

import com.cibertec.conecta.dto.inscripcion.InscripcionAdminResponse;
import com.cibertec.conecta.dto.inscripcion.InscripcionResponse;
import com.cibertec.conecta.exception.ConflictoException;
import com.cibertec.conecta.exception.RecursoNoEncontradoException;
import com.cibertec.conecta.exception.ReglaNegocioException;
import com.cibertec.conecta.model.Evento;
import com.cibertec.conecta.model.Inscripcion;
import com.cibertec.conecta.model.enums.EstadoEvento;
import com.cibertec.conecta.model.enums.EstadoInscripcion;
import com.cibertec.conecta.repository.EventoRepository;
import com.cibertec.conecta.repository.InscripcionRepository;
import com.cibertec.conecta.security.FirebasePrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InscripcionService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final InscripcionRepository inscripciones;
    private final EventoRepository eventos;
    private final EventoService eventoService;
    private final ParticipanteService participantes;
    private final QrService qrService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InscripcionResponse inscribir(FirebasePrincipal principal, Integer idEvento) {
        var participante = participantes.requerirPerfil(principal);
        Evento evento = bloquearEvento(idEvento);
        if (inscripciones.existsByParticipanteIdUsuarioAndEventoIdEvento(participante.getIdUsuario(), idEvento)) {
            throw new ConflictoException("Ya tienes una inscripción para este evento", "INSCRIPCION_DUPLICADA");
        }

        LocalDateTime ahora = LocalDateTime.now(LIMA);
        if (!aceptaCupos(evento)) {
            throw new ReglaNegocioException("El evento no admite inscripciones", "EVENTO_NO_DISPONIBLE");
        }
        if (!evento.getFecha().atTime(evento.getHoraInicio()).isAfter(ahora)) {
            throw new ReglaNegocioException("El evento ya comenzó", "EVENTO_PASADO");
        }
        int ocupados = eventoService.contarInscritos(idEvento);
        if (evento.getEstado() == EstadoEvento.LLENO || ocupados >= evento.getAforo()) {
            throw new ReglaNegocioException("El evento ya no tiene cupos disponibles", "SIN_CUPOS");
        }

        byte[] sufijo = new byte[10];
        ALEATORIO.nextBytes(sufijo);
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setParticipante(participante);
        inscripcion.setEvento(evento);
        inscripcion.setCodigo("CON-" + ahora.getYear() + "-" + HexFormat.of().withUpperCase().formatHex(sufijo));
        inscripcion.setQrToken(UUID.randomUUID().toString());
        inscripcion.setFechaInscripcion(ahora);
        inscripciones.save(inscripcion);

        actualizarCupos(evento, ocupados + 1);
        return toResponse(inscripcion);
    }

    @Transactional(readOnly = true)
    public List<InscripcionResponse> listarMias(FirebasePrincipal principal) {
        var participante = participantes.requerirPerfil(principal);
        return inscripciones.findByParticipanteIdUsuarioOrderByFechaInscripcionDesc(participante.getIdUsuario())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InscripcionResponse obtenerMia(FirebasePrincipal principal, Integer id) {
        return toResponse(buscarPropia(principal, id));
    }

    @Transactional(readOnly = true)
    public List<InscripcionAdminResponse> listarPorEvento(Integer idEvento) {
        if (!eventos.existsById(idEvento)) {
            throw new RecursoNoEncontradoException("Evento no encontrado");
        }
        return inscripciones.findByEventoIdEventoOrderByFechaInscripcionAsc(idEvento)
                .stream().map(inscripcion -> {
                    var participante = inscripcion.getParticipante();
                    return new InscripcionAdminResponse(toResponse(inscripcion), participante.getNombres(),
                            participante.getApellidos(), participante.getEmail(), participante.getTelefono());
                }).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InscripcionResponse cancelar(FirebasePrincipal principal, Integer id) {
        Integer idEvento = inscripciones.findEventoIdByInscripcionId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada"));
        // Se bloquea primero el evento, igual que en las operaciones administrativas.
        Evento evento = bloquearEvento(idEvento);
        Inscripcion inscripcion = buscarPropia(principal, id);
        cancelarConfirmada(evento, inscripcion, "Cancelada por el participante");
        return toResponse(inscripcion);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelarAdministrativamente(Integer idEvento, Integer idInscripcion) {
        Evento evento = bloquearEvento(idEvento);
        Inscripcion inscripcion = inscripciones.findByIdConDetalle(idInscripcion)
                .filter(item -> item.getEvento().getIdEvento().equals(idEvento))
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada en el evento"));
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            return;
        }
        cancelarConfirmada(evento, inscripcion, "Cancelada por administración");
    }

    private void cancelarConfirmada(Evento evento, Inscripcion inscripcion, String motivo) {
        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA || !aceptaCupos(evento)) {
            throw new ReglaNegocioException("Solo se pueden cancelar inscripciones confirmadas de eventos activos",
                    "ESTADO_INVALIDO");
        }
        if (!evento.getFecha().atTime(evento.getHoraInicio()).isAfter(LocalDateTime.now(LIMA))) {
            throw new ReglaNegocioException("El evento ya comenzó y la inscripción no puede cancelarse",
                    "EVENTO_INICIADO");
        }

        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        inscripcion.setObservacion(motivo);
        actualizarCupos(evento, eventoService.contarInscritos(evento.getIdEvento()));
    }

    @Transactional(readOnly = true)
    public byte[] obtenerQr(FirebasePrincipal principal, Integer id) {
        Inscripcion inscripcion = buscarPropia(principal, id);
        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA) {
            throw new ReglaNegocioException("El QR solo está disponible para inscripciones confirmadas",
                    "QR_NO_DISPONIBLE");
        }
        return qrService.generarPng(inscripcion.getCodigo(), inscripcion.getQrToken());
    }

    private Evento bloquearEvento(Integer id) {
        return eventos.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
    }

    private Inscripcion buscarPropia(FirebasePrincipal principal, Integer id) {
        var participante = participantes.requerirPerfil(principal);
        Inscripcion inscripcion = inscripciones.findByIdConDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada"));
        if (!participante.getIdUsuario().equals(inscripcion.getParticipante().getIdUsuario())) {
            throw new ReglaNegocioException("La inscripción pertenece a otro participante", "INSCRIPCION_AJENA");
        }
        return inscripcion;
    }

    private boolean aceptaCupos(Evento evento) {
        return evento.getEstado() == EstadoEvento.PUBLICADO || evento.getEstado() == EstadoEvento.LLENO;
    }

    private void actualizarCupos(Evento evento, int ocupados) {
        evento.setInscritos(ocupados);
        evento.setEstado(ocupados == evento.getAforo() ? EstadoEvento.LLENO : EstadoEvento.PUBLICADO);
    }

    private InscripcionResponse toResponse(Inscripcion inscripcion) {
        Evento evento = inscripcion.getEvento();
        return new InscripcionResponse(inscripcion.getIdInscripcion(), inscripcion.getCodigo(),
                inscripcion.getEstado().name(), inscripcion.getFechaInscripcion(), inscripcion.getFechaAsistencia(),
                new InscripcionResponse.EventoResumen(evento.getIdEvento(), evento.getTitulo(), evento.getFecha(),
                        evento.getHoraInicio(), evento.getUbicacion(), evento.getImagenUrl()),
                inscripcion.getEstado() == EstadoInscripcion.CONFIRMADA);
    }
}
