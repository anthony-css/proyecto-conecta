package com.cibertec.conecta.dto.inscripcion;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ValidarQrRequest(
        @NotBlank(message = "El token QR es obligatorio")
        @Size(max = 512, message = "El contenido QR es demasiado largo") String qrToken,
        @Positive(message = "El identificador del evento debe ser mayor que cero") Integer idEvento
) {}
