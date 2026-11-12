package com.cibertec.conecta.repository;

import com.cibertec.conecta.model.Participante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipanteRepository extends JpaRepository<Participante, Integer> {

    Optional<Participante> findByFirebaseUid(String firebaseUid);

    Optional<Participante> findByEmailIgnoreCase(String email);

    Optional<Participante> findByFcmToken(String fcmToken);

    boolean existsByEmailIgnoreCase(String email);
}
