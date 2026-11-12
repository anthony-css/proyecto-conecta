package com.cibertec.conecta.security;

import com.cibertec.conecta.dto.comun.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean esFirebase = path.equals("/api/participante") || path.startsWith("/api/participante/");
        response.setHeader("WWW-Authenticate", esFirebase ? "Bearer" : "Basic realm=\"Conecta\"");
        response.setHeader("Cache-Control", "no-store");
        ApiError error = esFirebase
                ? ApiError.of("Token de Firebase invalido o expirado",
                              "TOKEN_INVALIDO",
                              "Inicia sesion nuevamente en la app")
                : ApiError.of("Credenciales requeridas",
                              "NO_AUTENTICADO",
                              "Debe enviar un usuario y contrasena validos");

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}

