package com.cibertec.conecta.service;

import com.cibertec.conecta.dto.dashboard.DashboardResponse;
import com.cibertec.conecta.model.enums.EstadoEvento;
import com.cibertec.conecta.model.enums.EstadoInscripcion;
import com.cibertec.conecta.repository.AsistenciaRepository;
import com.cibertec.conecta.repository.EventoRepository;
import com.cibertec.conecta.repository.InscripcionRepository;
import com.cibertec.conecta.repository.ParticipanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final EventoRepository eventos;
    private final InscripcionRepository inscripciones;
    private final AsistenciaRepository asistencias;
    private final ParticipanteRepository participantes;

    @Transactional(readOnly = true)
    public DashboardResponse resumen() {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
        return new DashboardResponse(eventos.count(), eventos.countByEstado(EstadoEvento.PUBLICADO),
                eventos.contarProximos(List.of(EstadoEvento.PUBLICADO, EstadoEvento.LLENO),
                        ahora.toLocalDate(), ahora.toLocalTime()),
                inscripciones.countByEstadoIn(List.of(EstadoInscripcion.CONFIRMADA, EstadoInscripcion.ASISTIO)),
                asistencias.count(), participantes.count());
    }
}
