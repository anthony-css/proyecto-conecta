package com.cibertec.conecta.dto.categoria;

public record CategoriaResponse(
        Integer idCategoria,
        String nombre,
        String descripcion,
        String color,
        String icono,
        Boolean activo
) {}
