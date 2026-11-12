package com.cibertec.conecta.service;

import com.cibertec.conecta.dto.inscripcion.AsistenciaManualRequest;
import com.cibertec.conecta.dto.inscripcion.ValidacionResponse;
import com.cibertec.conecta.dto.inscripcion.ValidarQrRequest;
import com.cibertec.conecta.exception.ConflictoException;
import com.cibertec.conecta.exception.RecursoNoEncontradoException;
import com.cibertec.conecta.exception.ReglaNegocioException;
import com.cibertec.conecta.model.Asistencia;
import com.cibertec.conecta.model.Evento;
import com.cibertec.conecta.model.Inscripcion;
import com.cibertec.conecta.model.enums.EstadoEvento;
import com.cibertec.conecta.model.enums.EstadoInscripcion;
import com.cibertec.conecta.model.enums.MetodoAsistencia;
import com.cibertec.conecta.repository.AdministradorRepository;
import com.cibertec.conecta.repository.AsistenciaRepository;
import com.cibertec.conecta.repository.EventoRepository;
import com.cibertec.conecta.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsistenciaService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final AsistenciaRepository asistencias;
    private final InscripcionRepository inscripciones;
    private final EventoRepository eventos;
    private final AdministradorRepository administradores;
    private final QrService qrService;

    @Value("${app.asistencia.validar-solo-dia-evento}")
    private boolean validarSoloDiaEvento;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ValidacionResponse validarQr(ValidarQrRequest request, Integer idAdmin) {
        String token = qrService.extraerToken(request.qrToken());
        Integer id = inscripciones.findIdByQrToken(token).orElseThrow(this::qrInvalido);
        Inscripcion inscripcion = bloquearYConsultar(id);
        if (!token.equals(inscripcion.getQrToken())) {
            throw qrInvalido();
        }
        if (request.idEvento() != null && !request.idEvento().equals(inscripcion.getEvento().getIdEvento())) {
            throw new ReglaNegocioException("El código QR pertenece a otro evento", "QR_OTRO_EVENTO");
        }
        return registrar(inscripcion, MetodoAsistencia.QR, idAdmin, null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ValidacionResponse validarManual(Integer id, AsistenciaManualRequest request, Integer idAdmin) {
        String observacion = request.observacion();
        return registrar(bloquearYConsultar(id), MetodoAsistencia.MANUAL, idAdmin,
                observacion == null || observacion.isBlank() ? null : observacion.strip());
    }

    @Transactional(readOnly = true)
    public List<ValidacionResponse> listarPorEvento(Integer idEvento) {
        if (!eventos.existsById(idEvento)) {
            throw new RecursoNoEncontradoException("Evento no encontrado");
        }
        return asistencias.findByInscripcionEventoIdEventoOrderByFechaHoraAscIdAsistenciaAsc(idEvento)
                .stream().map(asistencia -> toResponse(asistencia, "REGISTRADA", asistencia.getObservacion())).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deshacer(Integer idEvento, Integer idAsistencia) {
        Evento evento = bloquearEvento(idEvento);
        validarEstadoEvento(evento);
        var asistencia = asistencias.findByIdAsistenciaAndInscripcionEventoIdEvento(idAsistencia, idEvento);
        if (asistencia.isEmpty()) {
            return;
        }
        Asistencia registro = asistencia.get();
        Inscripcion inscripcion = registro.getInscripcion();
        if (inscripcion.getEstado() != EstadoInscripcion.ASISTIO) {
            throw new ReglaNegocioException("La inscripción no está marcada como asistida", "ESTADO_INVALIDO");
        }
        inscripcion.setEstado(EstadoInscripcion.CONFIRMADA);
        inscripcion.setFechaAsistencia(null);
        asistencias.delete(registro);
    }

    private Inscripcion bloquearYConsultar(Integer id) {
        Integer idEvento = inscripciones.findEventoIdByInscripcionId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada"));
        // Las reservas, cancelaciones y asistencias adquieren el mismo bloqueo antes de modificar una inscripción.
        bloquearEvento(idEvento);
        return inscripciones.findByIdConDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada"));
    }

    private Evento bloquearEvento(Integer idEvento) {
        return eventos.findByIdForUpdate(idEvento)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
    }

    private ValidacionResponse registrar(Inscripcion inscripcion, MetodoAsistencia metodo, Integer idAdmin,
                                         String observacion) {
        if (inscripcion.getEstado() == EstadoInscripcion.ASISTIO
                || asistencias.existsByInscripcionIdInscripcion(inscripcion.getIdInscripcion())) {
            throw new ConflictoException("La asistencia ya fue registrada", "ASISTENCIA_DUPLICADA");
        }
        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA) {
            throw new ReglaNegocioException("La inscripción no está confirmada", "INSCRIPCION_INACTIVA");
        }

        Evento evento = inscripcion.getEvento();
        validarEstadoEvento(evento);
        LocalDateTime ahora = LocalDateTime.now(LIMA).withNano(0);
        validarVentana(evento, ahora);

        Asistencia asistencia = new Asistencia();
        asistencia.setInscripcion(inscripcion);
        asistencia.setAdminValidador(administradores.getReferenceById(idAdmin));
        asistencia.setMetodo(metodo);
        asistencia.setFechaHora(ahora);
        asistencia.setObservacion(observacion);
        asistencias.save(asistencia);
        inscripcion.setEstado(EstadoInscripcion.ASISTIO);
        inscripcion.setFechaAsistencia(ahora);
        return toResponse(asistencia, "OK", "Asistencia registrada correctamente");
    }

    private void validarEstadoEvento(Evento evento) {
        if (evento.getEstado() != EstadoEvento.PUBLICADO && evento.getEstado() != EstadoEvento.LLENO) {
            throw new ReglaNegocioException("El evento no admite cambios de asistencia", "EVENTO_NO_VALIDABLE");
        }
    }

    private void validarVentana(Evento evento, LocalDateTime ahora) {
        if (!validarSoloDiaEvento) {
            return;
        }
        if (!evento.getFecha().equals(ahora.toLocalDate())) {
            throw new ReglaNegocioException("La asistencia solo puede validarse el día del evento", "FUERA_DE_FECHA");
        }
        if (ahora.isBefore(evento.getFecha().atTime(evento.getHoraInicio()).minusHours(1))
                || ahora.isAfter(evento.getFecha().atTime(evento.getHoraFin()).plusHours(2))) {
            throw new ReglaNegocioException("La validación está fuera del horario permitido", "FUERA_DE_HORARIO");
        }
    }

    private ValidacionResponse toResponse(Asistencia asistencia, String resultado, String mensaje) {
        Inscripcion inscripcion = asistencia.getInscripcion();
        var participante = inscripcion.getParticipante();
        return new ValidacionResponse(resultado, mensaje,
                new ValidacionResponse.InscripcionValidada(inscripcion.getIdInscripcion(), inscripcion.getCodigo(),
                        participante.nombreCompleto(), participante.getEmail()),
                asistencia.getFechaHora(), asistencia.getMetodo().name(), asistencia.getIdAsistencia());
    }

    private ReglaNegocioException qrInvalido() {
        return new ReglaNegocioException("El código QR no corresponde a una inscripción válida", "QR_INVALIDO");
    }
}
