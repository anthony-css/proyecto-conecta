package com.cibertec.conecta.controller;

import com.cibertec.conecta.dto.auth.LoginResponse;
import com.cibertec.conecta.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * El filtro HTTP Basic ya valido las credenciales antes de entrar aqui.
     * Solo se devuelve el perfil para que Angular inicialice su sesion.
     */
    @PostMapping("/login")
    public LoginResponse login(Authentication authentication) {
        return authService.perfilAdministrador(authentication.getName());
    }
}
