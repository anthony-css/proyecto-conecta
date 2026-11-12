package com.cibertec.conecta.repository;

import com.cibertec.conecta.model.Inscripcion;
import com.cibertec.conecta.model.enums.EstadoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

    boolean existsByParticipanteIdUsuarioAndEventoIdEvento(Integer idParticipante, Integer idEvento);

    Optional<Inscripcion> findByParticipanteIdUsuarioAndEventoIdEvento(Integer idParticipante, Integer idEvento);

    @EntityGraph(attributePaths = "evento")
    List<Inscripcion> findByParticipanteIdUsuarioOrderByFechaInscripcionDesc(Integer idParticipante);

    @Query("select i.evento.idEvento from Inscripcion i where i.idInscripcion = :id")
    Optional<Integer> findEventoIdByInscripcionId(@Param("id") Integer id);

    @Query("select i.idInscripcion from Inscripcion i where i.qrToken = :token")
    Optional<Integer> findIdByQrToken(@Param("token") String token);

    @EntityGraph(attributePaths = {"participante", "evento"})
    List<Inscripcion> findByEventoIdEventoOrderByFechaInscripcionAsc(Integer idEvento);

    Optional<Inscripcion> findByQrToken(String qrToken);

    long countByEventoIdEventoAndEstadoIn(Integer idEvento, Collection<EstadoInscripcion> estados);

    long countByEstadoIn(Collection<EstadoInscripcion> estados);

    @Query("""
            select i from Inscripcion i
            join fetch i.participante p
            join fetch i.evento e
            where i.idInscripcion = :id
            """)
    Optional<Inscripcion> findByIdConDetalle(@Param("id") Integer id);

    @Query("""
            select i from Inscripcion i
            join fetch i.participante p
            join fetch i.evento e
            where i.qrToken = :qrToken
            """)
    Optional<Inscripcion> findByQrTokenConDetalle(@Param("qrToken") String qrToken);

    @Query("""
            select p.fcmToken from Inscripcion i
            join i.participante p
            where i.evento.idEvento = :idEvento
              and i.estado in :estados
              and p.fcmToken is not null
              and p.fcmToken <> ''
            """)
    List<String> findFcmTokensByEvento(@Param("idEvento") Integer idEvento,
                                       @Param("estados") Collection<EstadoInscripcion> estados);
}
