package com.cibertec.conecta.dto.dashboard;

public record DashboardResponse(
        long eventosTotales,
        long eventosPublicados,
        long eventosProximos,
        long inscripcionesActivas,
        long asistencias,
        long participantes
) {}
