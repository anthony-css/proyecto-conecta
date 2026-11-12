package com.cibertec.conecta.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio") @Size(max = 100, message = "El usuario no debe superar 100 caracteres") String username,
        @NotBlank(message = "La contrasena es obligatoria") String password
) {}
