package com.cibertec.conecta.dto.inscripcion;

import java.time.LocalDateTime;

public record ValidacionResponse(
        String resultado,
        String mensaje,
        InscripcionValidada inscripcion,
        LocalDateTime fechaHora,
        String metodo,
        Integer idAsistencia
) {
    public record InscripcionValidada(
            Integer idInscripcion,
            String codigo,
            String participante,
            String email
    ) {}
}
