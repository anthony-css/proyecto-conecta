package com.cibertec.conecta.controller;

import com.cibertec.conecta.dto.participante.ParticipanteResponse;
import com.cibertec.conecta.dto.participante.SincronizarParticipanteRequest;
import com.cibertec.conecta.security.FirebasePrincipal;
import com.cibertec.conecta.service.ParticipanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/participante/perfil")
@RequiredArgsConstructor
public class ParticipanteController {
    private final ParticipanteService participantes;

    @GetMapping
    public ParticipanteResponse perfil(@AuthenticationPrincipal FirebasePrincipal principal) {
        return participantes.perfil(principal);
    }

    @PutMapping
    public ParticipanteResponse sincronizar(@AuthenticationPrincipal FirebasePrincipal principal,
                                           @Valid @RequestBody SincronizarParticipanteRequest request) {
        return participantes.sincronizar(principal, request);
    }
}
