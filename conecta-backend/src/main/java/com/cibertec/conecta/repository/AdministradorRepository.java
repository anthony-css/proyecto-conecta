package com.cibertec.conecta.repository;

import com.cibertec.conecta.model.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Integer> {

    Optional<Administrador> findByEmail(String email);

    long countByActivoTrue();
}
