package com.cibertec.conecta.controller;

import com.cibertec.conecta.dto.categoria.CategoriaRequest;
import com.cibertec.conecta.dto.categoria.CategoriaResponse;
import com.cibertec.conecta.service.CategoriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/categorias")
@RequiredArgsConstructor
public class AdminCategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public List<CategoriaResponse> listar(
            @RequestParam(name = "solo_activas", defaultValue = "false") boolean soloActivas) {
        return categoriaService.listar(soloActivas);
    }

    @GetMapping("/{id}")
    public CategoriaResponse detalle(@PathVariable @Positive(message = "El identificador debe ser mayor que cero") Integer id) {
        return categoriaService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse response = categoriaService.crear(request);
        return ResponseEntity
                .created(URI.create("/api/admin/categorias/" + response.idCategoria()))
                .body(response);
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@PathVariable @Positive(message = "El identificador debe ser mayor que cero") Integer id,
                                        @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive(message = "El identificador debe ser mayor que cero") Integer id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
