package com.cibertec.conecta.security;

import com.cibertec.conecta.model.Usuario;
import com.cibertec.conecta.model.enums.Rol;
import com.cibertec.conecta.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));

        if (!(usuario instanceof com.cibertec.conecta.model.Administrador) || usuario.getRol() != Rol.ROLE_ADMIN || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsernameNotFoundException("Credenciales invalidas");
        }

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .roles("ADMIN")
                .build();
    }
}
