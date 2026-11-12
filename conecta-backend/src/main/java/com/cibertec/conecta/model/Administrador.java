package com.cibertec.conecta.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "administrador")
@PrimaryKeyJoinColumn(name = "id_usuario")
@Getter
@Setter

public class Administrador extends Usuario {

    public Administrador() {
        setRol(com.cibertec.conecta.model.enums.Rol.ROLE_ADMIN);
    }

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(length = 60)
    private String cargo;
}
