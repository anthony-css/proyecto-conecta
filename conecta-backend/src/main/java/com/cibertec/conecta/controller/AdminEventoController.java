package com.cibertec.conecta.controller;

import com.cibertec.conecta.dto.comun.PageResponse;
import com.cibertec.conecta.dto.evento.EventoRequest;
import com.cibertec.conecta.dto.evento.EventoResponse;
import com.cibertec.conecta.dto.inscripcion.InscripcionAdminResponse;
import com.cibertec.conecta.service.InscripcionService;

import com.cibertec.conecta.model.enums.EstadoEvento;
import com.cibertec.conecta.service.EventoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/eventos")
@RequiredArgsConstructor
public class AdminEventoController {

    private final EventoService eventoService;
    private final InscripcionService inscripciones;

    @GetMapping("/{id}/inscripciones")
    public List<InscripcionAdminResponse> listarInscripciones(@PathVariable @Positive Integer id) {
        return inscripciones.listarPorEvento(id);
    }

    @DeleteMapping("/{id}/inscripciones/{idInscripcion}")
    public ResponseEntity<Void> cancelarInscripcion(@PathVariable @Positive Integer id,
                                                     @PathVariable @Positive Integer idInscripcion) {
        inscripciones.cancelarAdministrativamente(id, idInscripcion);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public PageResponse<EventoResponse> listar(
            @RequestParam(required = false) EstadoEvento estado,
            @RequestParam(name = "id_categoria", required = false) @Positive(message = "La categoria debe ser mayor que cero") Integer idCategoria,
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return eventoService.listarAdmin(estado, idCategoria, q, fecha, pageable);
    }

    @GetMapping("/{id}")
    public EventoResponse detalle(@PathVariable @Positive(message = "El identificador debe ser mayor que cero") Integer id) {
        return eventoService.obtenerAdmin(id);
    }

    @GetMapping("/para-asistencia")
    public PageResponse<EventoResponse> listarParaAsistencia(@RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = {"fecha", "horaInicio", "idEvento"}) Pageable pageable) {
        return eventoService.listarParaAsistencia(q, pageable);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventoResponse> crear(
            @Valid @RequestBody EventoRequest datos) {

        EventoResponse response = eventoService.crear(datos);

        return ResponseEntity
                .created(URI.create("/api/admin/eventos/" + response.idEvento()))
                .body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EventoResponse actualizar(
            @PathVariable @Positive(message = "El identificador debe ser mayor que cero") Integer id,
            @Valid @RequestBody EventoRequest datos) {

        return eventoService.actualizar(id, datos);
    }

    @PatchMapping("/{id}/publicar")
    public EventoResponse publicar(@PathVariable @Positive(message = "El identificador debe ser mayor que cero") Integer id) {
        return eventoService.publicar(id);
    }

    @PatchMapping("/{id}/cancelar")
    public EventoResponse cancelar(@PathVariable @Positive(message = "El identificador debe ser mayor que cero") Integer id) {
        return eventoService.cancelar(id);
    }

    @PatchMapping("/{id}/realizar")
    public EventoResponse marcarRealizado(@PathVariable @Positive Integer id) {
        return eventoService.marcarRealizado(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive(message = "El identificador debe ser mayor que cero") Integer id) {
        eventoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}


