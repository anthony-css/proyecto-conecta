package com.cibertec.conecta.security;

import com.cibertec.conecta.dto.comun.ApiError;
import com.cibertec.conecta.model.enums.Rol;
import com.cibertec.conecta.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final FirebaseTokenVerifier verifier;
    private final UsuarioRepository usuarios;
    private final RestAuthenticationEntryPoint entryPoint;
    private final RestAccessDeniedHandler deniedHandler;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(request, response);
            return;
        }

        FirebasePrincipal principal;
        try {
            String token = header.substring(7).trim();
            if (token.isEmpty()) {
                throw new BadCredentialsException("Token ausente");
            }
            principal = verifier.verificar(token);
            var usuario = usuarios.findByFirebaseUid(principal.uid());
            if (usuario.isPresent() && (!Boolean.TRUE.equals(usuario.get().getActivo())
                    || usuario.get().getRol() != Rol.ROLE_PARTICIPANTE)) {
                SecurityContextHolder.clearContext();
                deniedHandler.handle(request, response,
                        new org.springframework.security.access.AccessDeniedException("Participante deshabilitado"));
                return;
            }
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, ex);
            return;
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            log.error("No se pudo verificar la identidad del participante: {}", ex.getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            objectMapper.writeValue(response.getOutputStream(), ApiError.of(
                    "La autenticación de participantes no está disponible", "SERVICIO_NO_DISPONIBLE",
                    "Intente nuevamente más tarde"));
            return;
        }

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANTE"))));
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
