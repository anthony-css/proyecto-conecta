package com.cibertec.conecta.dto.inscripcion;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record InscripcionAdminResponse(
        @JsonUnwrapped InscripcionResponse inscripcion,
        String nombres,
        String apellidos,
        String email,
        String telefono
) {}
