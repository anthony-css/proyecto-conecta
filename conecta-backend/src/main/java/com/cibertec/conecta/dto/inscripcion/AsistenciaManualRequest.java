package com.cibertec.conecta.dto.inscripcion;

import jakarta.validation.constraints.Size;

public record AsistenciaManualRequest(
        @Size(max = 255, message = "No debe superar {max} caracteres") String observacion
) {}
