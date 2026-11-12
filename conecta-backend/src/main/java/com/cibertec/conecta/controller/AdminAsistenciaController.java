package com.cibertec.conecta.controller;

import com.cibertec.conecta.dto.inscripcion.AsistenciaManualRequest;
import com.cibertec.conecta.dto.inscripcion.ValidacionResponse;
import com.cibertec.conecta.dto.inscripcion.ValidarQrRequest;
import com.cibertec.conecta.service.AsistenciaService;
import com.cibertec.conecta.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAsistenciaController {

    private final AsistenciaService asistenciaService;
    private final AuthService authService;

    @PostMapping("/asistencias/validar-qr")
    public ValidacionResponse validarQr(@Valid @RequestBody ValidarQrRequest request,
                                        Authentication authentication) {
        Integer idAdmin = authService.idAdministrador(authentication.getName());
        return asistenciaService.validarQr(request, idAdmin);
    }

    @PostMapping("/inscripciones/{id}/asistencia-manual")
    public ValidacionResponse validarManual(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody AsistenciaManualRequest request,
            Authentication authentication) {

        Integer idAdmin = authService.idAdministrador(authentication.getName());
        return asistenciaService.validarManual(id, request, idAdmin);
    }

    @GetMapping("/eventos/{id}/asistencias")
    public List<ValidacionResponse> listarPorEvento(@PathVariable @Positive Integer id) {
        return asistenciaService.listarPorEvento(id);
    }

    @DeleteMapping("/eventos/{id}/asistencias/{idAsistencia}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deshacer(@PathVariable @Positive Integer id, @PathVariable @Positive Integer idAsistencia) {
        asistenciaService.deshacer(id, idAsistencia);
    }
}
