package com.cibertec.conecta.controller;

import com.cibertec.conecta.dto.inscripcion.InscripcionRequest;
import com.cibertec.conecta.dto.inscripcion.InscripcionResponse;
import com.cibertec.conecta.security.FirebasePrincipal;
import com.cibertec.conecta.service.InscripcionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/participante/inscripciones")
@RequiredArgsConstructor
public class InscripcionParticipanteController {
    private final InscripcionService inscripciones;

    @PostMapping
    public ResponseEntity<InscripcionResponse> inscribir(@AuthenticationPrincipal FirebasePrincipal principal,
                                                        @Valid @RequestBody InscripcionRequest request) {
        InscripcionResponse respuesta = inscripciones.inscribir(principal, request.idEvento());
        return ResponseEntity.created(URI.create("/api/participante/inscripciones/" + respuesta.idInscripcion()))
                .body(respuesta);
    }

    @GetMapping
    public List<InscripcionResponse> listar(@AuthenticationPrincipal FirebasePrincipal principal) {
        return inscripciones.listarMias(principal);
    }

    @GetMapping("/{id}")
    public InscripcionResponse detalle(@AuthenticationPrincipal FirebasePrincipal principal,
                                      @PathVariable @Positive Integer id) {
        return inscripciones.obtenerMia(principal, id);
    }

    @DeleteMapping("/{id}")
    public InscripcionResponse cancelar(@AuthenticationPrincipal FirebasePrincipal principal,
                                       @PathVariable @Positive Integer id) {
        return inscripciones.cancelar(principal, id);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@AuthenticationPrincipal FirebasePrincipal principal,
                                    @PathVariable @Positive Integer id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .body(inscripciones.obtenerQr(principal, id));
    }
}
