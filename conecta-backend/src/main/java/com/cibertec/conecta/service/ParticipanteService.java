package com.cibertec.conecta.service;

import com.cibertec.conecta.dto.participante.ParticipanteResponse;
import com.cibertec.conecta.dto.participante.SincronizarParticipanteRequest;
import com.cibertec.conecta.exception.ConflictoException;
import com.cibertec.conecta.exception.ReglaNegocioException;
import com.cibertec.conecta.model.Participante;
import com.cibertec.conecta.model.Usuario;
import com.cibertec.conecta.model.enums.Rol;
import com.cibertec.conecta.repository.ParticipanteRepository;
import com.cibertec.conecta.repository.UsuarioRepository;
import com.cibertec.conecta.security.FirebasePrincipal;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParticipanteService {
    private final ParticipanteRepository participantes;
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public ParticipanteResponse perfil(FirebasePrincipal principal) {
        return toResponse(requerirPerfil(principal));
    }

    Participante requerirPerfil(FirebasePrincipal principal) {
        return buscar(principal)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El participante aún no ha sincronizado su perfil", "PERFIL_NO_SINCRONIZADO"));
    }

    @Transactional
    public ParticipanteResponse sincronizar(FirebasePrincipal principal, SincronizarParticipanteRequest request) {
        if (principal.email() == null || principal.email().isBlank()) {
            throw new ReglaNegocioException("La cuenta de Firebase no tiene correo asociado", "FIREBASE_SIN_EMAIL");
        }
        String correo = principal.email().strip().toLowerCase(Locale.ROOT);
        if (!correo.equals(request.email().strip().toLowerCase(Locale.ROOT))) {
            throw new ReglaNegocioException("El correo del perfil debe coincidir con la cuenta de Firebase",
                    "CORREO_NO_COINCIDE");
        }

        Participante participante = buscar(principal).orElseGet(() -> {
            Participante nuevo = new Participante();
            nuevo.setFirebaseUid(principal.uid());
            nuevo.setPassword("FIREBASE_AUTH");
            return nuevo;
        });
        Integer idActual = participante.getIdUsuario();
        if (participantes.findByEmailIgnoreCase(correo)
                .filter(p -> !Objects.equals(p.getIdUsuario(), idActual)).isPresent()) {
            throw correoDuplicado();
        }
        if (usuarios.findByUsername(correo)
                .filter(u -> !Objects.equals(u.getIdUsuario(), idActual)).isPresent()) {
            throw usuarioDuplicado();
        }

        participante.setUsername(correo);
        participante.setEmail(correo);
        participante.setNombres(request.nombres().strip());
        participante.setApellidos(request.apellidos().strip());
        participante.setTelefono(limpiar(request.telefono()));
        participante.setFotoUrl(limpiar(request.fotoUrl()));
        try {
            return toResponse(participantes.saveAndFlush(participante));
        } catch (DataIntegrityViolationException ex) {
            // Las restricciones únicas también cubren altas simultáneas.
            if (ex.getCause() instanceof ConstraintViolationException violation
                    && violation.getConstraintName() != null) {
                String clave = violation.getConstraintName();
                switch (clave.substring(clave.lastIndexOf('.') + 1)) {
                    case "uq_participante_email" -> throw correoDuplicado();
                    case "uq_usuario_username" -> throw usuarioDuplicado();
                    case "uq_usuario_firebase_uid" -> throw new ConflictoException(
                            "El participante ya fue registrado por otra petición", "PARTICIPANTE_DUPLICADO");
                }
            }
            throw ex;
        }
    }

    private Optional<Participante> buscar(FirebasePrincipal principal) {
        Optional<Usuario> usuario = usuarios.findByFirebaseUid(principal.uid());
        if (usuario.isEmpty()) {
            return Optional.empty();
        }
        // MySQL puede encontrar un UID con distinta capitalización según su collation.
        if (!(usuario.get() instanceof Participante participante)
                || !principal.uid().equals(participante.getFirebaseUid())
                || participante.getRol() != Rol.ROLE_PARTICIPANTE
                || !Boolean.TRUE.equals(participante.getActivo())) {
            throw new AccessDeniedException("El participante no tiene acceso al perfil");
        }
        return Optional.of(participante);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private ConflictoException correoDuplicado() {
        return new ConflictoException("El correo ya está registrado por otro participante", "CORREO_DUPLICADO");
    }

    private ConflictoException usuarioDuplicado() {
        return new ConflictoException("El correo ya está registrado como usuario", "USUARIO_DUPLICADO");
    }

    private ParticipanteResponse toResponse(Participante participante) {
        return new ParticipanteResponse(participante.getIdUsuario(), participante.getNombres(),
                participante.getApellidos(), participante.getEmail(), participante.getTelefono(),
                participante.getFotoUrl());
    }
}
