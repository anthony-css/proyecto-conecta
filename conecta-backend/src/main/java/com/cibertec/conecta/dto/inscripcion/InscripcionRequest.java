package com.cibertec.conecta.dto.inscripcion;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Positive;

public record InscripcionRequest(
        @NotNull(message = "El evento es obligatorio") @Positive(message = "El identificador del evento debe ser mayor que cero") Integer idEvento
) {}
