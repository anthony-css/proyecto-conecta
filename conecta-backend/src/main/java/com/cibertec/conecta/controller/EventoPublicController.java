package com.cibertec.conecta.controller;

import com.cibertec.conecta.dto.comun.PageResponse;
import com.cibertec.conecta.dto.evento.EventoResponse;
import com.cibertec.conecta.dto.evento.EventoResumenResponse;
import com.cibertec.conecta.service.EventoService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoPublicController {

    private final EventoService eventoService;

    @GetMapping
    public PageResponse<EventoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(name = "id_categoria", required = false) @Positive(message = "La categoria debe ser mayor que cero") Integer idCategoria,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @PageableDefault(size = 12, sort = "fecha") Pageable pageable) {

        return eventoService.listarPublicos(q, idCategoria, fecha, pageable);
    }

    @GetMapping("/destacados")
    public List<EventoResumenResponse> destacados() {
        return eventoService.destacados();
    }

    @GetMapping("/{id}")
    public EventoResponse detalle(@PathVariable @Positive(message = "El identificador debe ser mayor que cero") Integer id) {
        return eventoService.obtenerPublico(id);
    }
}
