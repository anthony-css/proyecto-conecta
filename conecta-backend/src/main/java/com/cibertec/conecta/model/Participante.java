package com.cibertec.conecta.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "participante")
@PrimaryKeyJoinColumn(name = "id_usuario")
@Getter
@Setter

public class Participante extends Usuario {

    public Participante() {
        setRol(com.cibertec.conecta.model.enums.Rol.ROLE_PARTICIPANTE);
    }

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }
}
