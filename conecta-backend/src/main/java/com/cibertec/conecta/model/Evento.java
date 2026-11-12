package com.cibertec.conecta.model;

import com.cibertec.conecta.model.enums.EstadoEvento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Integer idEvento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false, length = 150)
    private String ubicacion;

    @Column(length = 200)
    private String direccion;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(nullable = false)
    private Integer aforo;

    @Column(nullable = false)
    private Integer inscritos = 0;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoEvento estado = EstadoEvento.BORRADOR;

    @Column(nullable = false)
    private Boolean destacado = false;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }

    @Transient
    public int getCuposDisponibles() {
        return Math.max(0, aforo - inscritos);
    }

    @Transient
    public boolean isInscripcionAbierta() {
        return estado == EstadoEvento.PUBLICADO
                && getCuposDisponibles() > 0
                && fecha.atTime(horaInicio).isAfter(LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
    }
}
