package com.cibertec.conecta.dto.evento;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventoResumenResponse(
        Integer idEvento,
        String titulo,
        String imagenUrl,
        LocalDate fecha,
        LocalTime horaInicio,
        String ubicacion,
        Integer cuposDisponibles,
        Boolean destacado
) {}
