package com.cibertec.conecta.dto.comun;

import java.time.LocalDateTime;

public record ApiError(
        String mensaje,
        String codigo,
        String detalle,
        LocalDateTime timestamp
) {
    public static ApiError of(String mensaje, String codigo, String detalle) {
        return new ApiError(mensaje, codigo, detalle, LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
    }
}
