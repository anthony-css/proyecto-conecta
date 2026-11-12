package com.cibertec.conecta.dto.auth;

import com.cibertec.conecta.model.enums.Rol;

public record LoginResponse(
        Integer idUsuario,
        String username,
        Rol rol,
        String nombres,
        String apellidos,
        String email
) {}
