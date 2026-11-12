package com.cibertec.conecta.dto.evento;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

public record EventoResponse(
        Integer idEvento,
        String titulo,
        String descripcion,
        CategoriaResumen categoria,
        String imagenUrl,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String ubicacion,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        Integer aforo,
        Integer inscritos,
        Integer cuposDisponibles,
        String estado,
        Boolean destacado,
        Long version
) {
    public record CategoriaResumen(
            Integer idCategoria,
            String nombre,
            String color,
            String icono
    ) {}
}
