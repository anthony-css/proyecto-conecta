package com.cibertec.conecta.dto.inscripcion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record InscripcionResponse(
        Integer idInscripcion,
        String codigo,
        String estado,
        LocalDateTime fechaInscripcion,
        LocalDateTime fechaAsistencia,
        EventoResumen evento,
        Boolean qrDisponible
) {
    public record EventoResumen(
            Integer idEvento,
            String titulo,
            LocalDate fecha,
            LocalTime horaInicio,
            String ubicacion,
            String imagenUrl
    ) {}
}
