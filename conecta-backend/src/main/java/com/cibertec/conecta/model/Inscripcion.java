package com.cibertec.conecta.model;

import com.cibertec.conecta.model.enums.EstadoInscripcion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscripcion",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_inscripcion_participante_evento",
                             columnNames = {"id_participante", "id_evento"}),
           @UniqueConstraint(name = "uq_inscripcion_codigo",
                             columnNames = {"codigo"}),
           @UniqueConstraint(name = "uq_inscripcion_qr_token",
                             columnNames = {"qr_token"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscripcion")
    private Integer idInscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_participante", nullable = false)
    private Participante participante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "qr_token", nullable = false, unique = true, length = 64)
    private String qrToken;

    @Column(name = "fecha_inscripcion", nullable = false, updatable = false)
    private LocalDateTime fechaInscripcion = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado = EstadoInscripcion.CONFIRMADA;

    @Column(name = "fecha_asistencia")
    private LocalDateTime fechaAsistencia;

    @Column(length = 255)
    private String observacion;
}
