package com.cibertec.conecta.dto.participante;

public record ParticipanteResponse(
        Integer idUsuario,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        String fotoUrl
) {}
