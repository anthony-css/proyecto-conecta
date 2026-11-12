package com.cibertec.conecta.dto.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "No debe superar {max} caracteres")
        String nombre,

        @Size(max = 200, message = "No debe superar {max} caracteres")
        String descripcion,

        @Size(max = 20, message = "No debe superar {max} caracteres")
        String color,

        @Size(max = 60, message = "No debe superar {max} caracteres")
        String icono,

        Boolean activo
) {}
