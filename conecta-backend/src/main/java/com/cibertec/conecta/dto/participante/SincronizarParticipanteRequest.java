package com.cibertec.conecta.dto.participante;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SincronizarParticipanteRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "No debe superar {max} caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "No debe superar {max} caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Size(max = 100, message = "No debe superar {max} caracteres")
        String email,

        @Size(max = 20, message = "No debe superar {max} caracteres")
        String telefono,

        @Size(max = 255, message = "No debe superar {max} caracteres")
        String fotoUrl
) {}
