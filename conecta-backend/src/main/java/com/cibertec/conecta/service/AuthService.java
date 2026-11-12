package com.cibertec.conecta.service;

import com.cibertec.conecta.dto.auth.LoginResponse;
import com.cibertec.conecta.model.Administrador;
import com.cibertec.conecta.model.enums.Rol;
import com.cibertec.conecta.repository.AdministradorRepository;
import com.cibertec.conecta.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;

    @Transactional(readOnly = true)
    public LoginResponse perfilAdministrador(String username) {
        Administrador admin = administradorActivo(username);
        return new LoginResponse(admin.getIdUsuario(), admin.getUsername(), admin.getRol(),
                admin.getNombres(), admin.getApellidos(), admin.getEmail());
    }

    @Transactional(readOnly = true)
    public Integer idAdministrador(String username) {
        return administradorActivo(username).getIdUsuario();
    }

    private Administrador administradorActivo(String username) {
        var usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (usuario.getRol() != Rol.ROLE_ADMIN || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return administradorRepository.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
    }
}
