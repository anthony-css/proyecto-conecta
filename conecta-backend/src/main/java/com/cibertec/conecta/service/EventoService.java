package com.cibertec.conecta.service;

import com.cibertec.conecta.dto.comun.PageResponse;
import com.cibertec.conecta.dto.evento.EventoRequest;
import com.cibertec.conecta.dto.evento.EventoResponse;
import com.cibertec.conecta.dto.evento.EventoResumenResponse;
import com.cibertec.conecta.exception.ConflictoException;
import com.cibertec.conecta.exception.RecursoNoEncontradoException;
import com.cibertec.conecta.exception.ReglaNegocioException;
import com.cibertec.conecta.model.Categoria;
import com.cibertec.conecta.model.Evento;
import com.cibertec.conecta.model.enums.EstadoEvento;
import com.cibertec.conecta.repository.CategoriaRepository;
import com.cibertec.conecta.repository.EventoRepository;
import com.cibertec.conecta.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.cibertec.conecta.model.enums.EstadoInscripcion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoService {

    private static final List<EstadoEvento> ESTADOS_PUBLICOS =
            List.of(EstadoEvento.PUBLICADO, EstadoEvento.LLENO);

    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InscripcionRepository inscripcionRepository;

    // ------------------------------------------------------------------
    // Consultas publicas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<EventoResponse> listarPublicos(String q,
                                                       Integer idCategoria,
                                                       LocalDate fecha,
                                                       Pageable pageable) {
        LocalDateTime ahora = ahora();
        Page<Evento> pagina = eventoRepository.buscarPorEstados(
                ESTADOS_PUBLICOS, idCategoria, normalizar(q), fecha, ahora.toLocalDate(),
                ahora.toLocalTime(), ordenar(pageable));
        return PageResponse.from(pagina, this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventoResponse obtenerPublico(Integer id) {
        Evento evento = eventoRepository.findByIdConCategoria(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));

        if (!ESTADOS_PUBLICOS.contains(evento.getEstado()) || !fin(evento).isAfter(ahora())) {
            throw new RecursoNoEncontradoException("Evento no encontrado");
        }
        return toResponse(evento);
    }

    @Transactional(readOnly = true)
    public List<EventoResumenResponse> destacados() {
        LocalDateTime ahora = ahora();
        return eventoRepository
                .buscarDestacados(ESTADOS_PUBLICOS, ahora.toLocalDate(), ahora.toLocalTime(), PageRequest.of(0, 5))
                .stream()
                .map(this::toResumen)
                .toList();
    }

    // ------------------------------------------------------------------
    // Administracion
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<EventoResponse> listarAdmin(EstadoEvento estado,
                                                    Integer idCategoria,
                                                    String q,
                                                    LocalDate fecha,
                                                    Pageable pageable) {
        Page<Evento> pagina = eventoRepository.buscar(
                estado, idCategoria, normalizar(q), fecha, ordenar(pageable));
        return PageResponse.from(pagina, this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventoResponse obtenerAdmin(Integer id) {
        return toResponse(buscarConCategoria(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<EventoResponse> listarParaAsistencia(String q, Pageable pageable) {
        return PageResponse.from(eventoRepository.buscarPorEstados(
                ESTADOS_PUBLICOS, null, normalizar(q), null, null, null, ordenar(pageable)),
                this::toResponse);
    }

    @Transactional
    public EventoResponse crear(EventoRequest request) {
        validarHorario(request);
        Categoria categoria = buscarCategoriaActiva(request.getIdCategoria());

        Evento evento = new Evento();
        evento.setCategoria(categoria);
        aplicar(evento, request);

        evento.setInscritos(0);
        evento.setEstado(EstadoEvento.BORRADOR);

        eventoRepository.saveAndFlush(evento);
        return toResponse(evento);
    }

    @Transactional
    public EventoResponse actualizar(Integer id, EventoRequest request) {
        Evento evento = eventoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));

        validarEditable(evento);
        if (request.getVersion() != null && !request.getVersion().equals(evento.getVersion())) {
            throw new ConflictoException("El evento fue modificado por otra operación", "VERSION_CONFLICTO");
        }
        validarHorario(request);

        int inscritos = contarInscritos(id);

        if (request.getAforo() < inscritos) {
            throw new ReglaNegocioException(
                    "El aforo no puede ser menor que las inscripciones confirmadas",
                    "AFORO_INVALIDO");
        }

        Categoria categoria = buscarCategoriaActiva(request.getIdCategoria());

        if (ESTADOS_PUBLICOS.contains(evento.getEstado())) {
            validarInicioFuturo(request.getFecha().atTime(request.getHoraInicio()));
        }

        evento.setCategoria(categoria);
        aplicar(evento, request);
        evento.setInscritos(inscritos);

        if (ESTADOS_PUBLICOS.contains(evento.getEstado())) {
            evento.setEstado(evento.getCuposDisponibles() > 0 ? EstadoEvento.PUBLICADO : EstadoEvento.LLENO);
        }

        eventoRepository.saveAndFlush(evento);
        return toResponse(evento);
    }

    @Transactional
    public EventoResponse publicar(Integer id) {
        Evento evento = eventoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));

        if (evento.getEstado() == EstadoEvento.PUBLICADO) {
            return toResponse(evento);
        }

        if (evento.getEstado() != EstadoEvento.BORRADOR) {
            throw new ReglaNegocioException(
                    "Solo un evento en borrador puede publicarse", "ESTADO_INVALIDO");
        }

        buscarCategoriaActiva(evento.getCategoria().getIdCategoria());
        validarInicioFuturo(evento.getFecha().atTime(evento.getHoraInicio()));
        evento.setInscritos(contarInscritos(id));

        if (evento.getCuposDisponibles() <= 0) {
            throw new ReglaNegocioException(
                    "No se puede publicar un evento sin cupos disponibles", "SIN_CUPOS");
        }

        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setFechaActualizacion(LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
        eventoRepository.saveAndFlush(evento);
        return toResponse(evento);
    }

    @Transactional
    public EventoResponse cancelar(Integer id) {
        Evento evento = eventoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));

        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            return toResponse(evento);
        }

        if (!ESTADOS_PUBLICOS.contains(evento.getEstado())) {
            throw new ReglaNegocioException(
                    "Solo un evento publicado o lleno puede cancelarse", "ESTADO_INVALIDO");
        }

        evento.setEstado(EstadoEvento.CANCELADO);
        cancelarConfirmadas(id);
        evento.setInscritos(contarInscritos(id));
        eventoRepository.saveAndFlush(evento);
        return toResponse(evento);
    }

    private void cancelarConfirmadas(Integer id) {
        inscripcionRepository.findByEventoIdEventoOrderByFechaInscripcionAsc(id)
                .stream()
                .filter(i -> i.getEstado() == com.cibertec.conecta.model.enums.EstadoInscripcion.CONFIRMADA)
                .forEach(i -> {
                    i.setEstado(com.cibertec.conecta.model.enums.EstadoInscripcion.CANCELADA);
                    i.setObservacion("Evento cancelado por el organizador");
                });
        inscripcionRepository.flush();
    }

    @Transactional
    public EventoResponse marcarRealizado(Integer id) {
        Evento evento = eventoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
        if (evento.getEstado() == EstadoEvento.REALIZADO) {
            return toResponse(evento);
        }
        if (!ESTADOS_PUBLICOS.contains(evento.getEstado())) {
            throw new ReglaNegocioException("Solo se puede cerrar un evento publicado o lleno", "ESTADO_INVALIDO");
        }
        LocalDateTime ahora = ahora();
        if (fin(evento).isAfter(ahora)) {
            throw new ReglaNegocioException("Espera a que termine el evento para marcarlo como realizado", "EVENTO_NO_TERMINADO");
        }

        for (var inscripcion : inscripcionRepository.findByEventoIdEventoOrderByFechaInscripcionAsc(id)) {
            if (inscripcion.getEstado() == EstadoInscripcion.CONFIRMADA) {
                inscripcion.setEstado(EstadoInscripcion.NO_ASISTIO);
            }
        }
        evento.setEstado(EstadoEvento.REALIZADO);
        evento.setFechaActualizacion(ahora);
        eventoRepository.flush();
        evento.setInscritos(contarInscritos(id));
        return toResponse(evento);
    }

    private LocalDateTime fin(Evento evento) {
        return evento.getFecha().atTime(evento.getHoraFin());
    }

    @Transactional
    public void eliminar(Integer id) {
        Evento evento = eventoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));

        if (!inscripcionRepository.findByEventoIdEventoOrderByFechaInscripcionAsc(id).isEmpty()) {
            // Baja logica: no se puede borrar un evento con historial.
            evento.setEstado(EstadoEvento.INACTIVO);
            cancelarConfirmadas(id);
            evento.setInscritos(contarInscritos(id));
            eventoRepository.saveAndFlush(evento);
            return;
        }

        eventoRepository.delete(evento);
        eventoRepository.flush();

    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Evento buscarConCategoria(Integer id) {
        return eventoRepository.findByIdConCategoria(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
    }

    private Categoria buscarCategoriaActiva(Integer idCategoria) {
        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada"));

        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ReglaNegocioException(
                    "La categoria seleccionada esta inactiva", "CATEGORIA_INACTIVA");
        }
        return categoria;
    }

    private void validarHorario(EventoRequest request) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new ReglaNegocioException(
                    "La hora de fin debe ser posterior a la hora de inicio", "HORARIO_INVALIDO");
        }
    }

    private void validarEditable(Evento evento) {
        if (evento.getEstado() == EstadoEvento.REALIZADO
                || evento.getEstado() == EstadoEvento.CANCELADO
                || evento.getEstado() == EstadoEvento.INACTIVO) {
            throw new ReglaNegocioException(
                    "El evento ya no puede modificarse", "ESTADO_INVALIDO");
        }
    }

    private void aplicar(Evento evento, EventoRequest request) {
        evento.setImagenUrl(request.getImagenUrl());
        evento.setTitulo(request.getTitulo().trim());
        evento.setDescripcion(request.getDescripcion().trim());
        evento.setFecha(request.getFecha());
        evento.setHoraInicio(request.getHoraInicio());
        evento.setHoraFin(request.getHoraFin());
        evento.setUbicacion(request.getUbicacion().trim());
        evento.setDireccion(request.getDireccion());
        evento.setLatitud(request.getLatitud());
        evento.setLongitud(request.getLongitud());
        evento.setAforo(request.getAforo());
        evento.setDestacado(Boolean.TRUE.equals(request.getDestacado()));
        evento.setFechaActualizacion(LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
    }

    private String normalizar(String q) {
        return (q == null || q.isBlank()) ? null : q.trim();
    }

    public EventoResponse toResponse(Evento e) {
        return new EventoResponse(
                e.getIdEvento(),
                e.getTitulo(),
                e.getDescripcion(),
                new EventoResponse.CategoriaResumen(
                        e.getCategoria().getIdCategoria(),
                        e.getCategoria().getNombre(),
                        e.getCategoria().getColor(),
                        e.getCategoria().getIcono()
                ),
                e.getImagenUrl(),
                e.getFecha(),
                e.getHoraInicio(),
                e.getHoraFin(),
                e.getUbicacion(),
                e.getDireccion(),
                e.getLatitud(),
                e.getLongitud(),
                e.getAforo(),
                e.getInscritos(),
                e.getCuposDisponibles(),
                e.getEstado().name(),
                e.getDestacado(),
                e.getVersion()
        );
    }

    private EventoResumenResponse toResumen(Evento e) {
        return new EventoResumenResponse(
                e.getIdEvento(),
                e.getTitulo(),
                e.getImagenUrl(),
                e.getFecha(),
                e.getHoraInicio(),
                e.getUbicacion(),
                e.getCuposDisponibles(),
                e.getDestacado()
        );
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }

    private void validarInicioFuturo(LocalDateTime inicio) {
        if (!inicio.isAfter(ahora())) {
            throw new ReglaNegocioException("La fecha y hora de inicio deben ser futuras", "FECHA_INVALIDA");
        }
    }

    int contarInscritos(Integer id) {
        return Math.toIntExact(inscripcionRepository.countByEventoIdEventoAndEstadoIn(id,
                List.of(EstadoInscripcion.CONFIRMADA, EstadoInscripcion.ASISTIO, EstadoInscripcion.NO_ASISTIO)));
    }

    private Pageable ordenar(Pageable pageable) {
        java.util.Map<String, String> campos = java.util.Map.ofEntries(
                java.util.Map.entry("id_evento", "idEvento"), java.util.Map.entry("idEvento", "idEvento"),
                java.util.Map.entry("fecha", "fecha"), java.util.Map.entry("titulo", "titulo"),
                java.util.Map.entry("hora_inicio", "horaInicio"), java.util.Map.entry("horaInicio", "horaInicio"),
                java.util.Map.entry("hora_fin", "horaFin"), java.util.Map.entry("horaFin", "horaFin"),
                java.util.Map.entry("estado", "estado"), java.util.Map.entry("aforo", "aforo"),
                java.util.Map.entry("inscritos", "inscritos"), java.util.Map.entry("destacado", "destacado"),
                java.util.Map.entry("fecha_creacion", "fechaCreacion"), java.util.Map.entry("fechaCreacion", "fechaCreacion"));
        List<Sort.Order> ordenes = pageable.getSort().stream().map(order -> {
            String campo = campos.get(order.getProperty());
            if (campo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo de ordenación inválido");
            }
            return order.withProperty(campo);
        }).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (ordenes.stream().noneMatch(order -> order.getProperty().equals("idEvento"))) {
            ordenes.add(Sort.Order.asc("idEvento"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(ordenes));
    }
}

