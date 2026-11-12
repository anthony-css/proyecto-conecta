package com.cibertec.conecta.repository;

import com.cibertec.conecta.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    Optional<Asistencia> findByInscripcionIdInscripcion(Integer idInscripcion);

    Optional<Asistencia> findByIdAsistenciaAndInscripcionEventoIdEvento(Integer idAsistencia, Integer idEvento);

    boolean existsByInscripcionIdInscripcion(Integer idInscripcion);
    @EntityGraph(attributePaths = {"inscripcion", "inscripcion.participante"})
    List<Asistencia> findByInscripcionEventoIdEventoOrderByFechaHoraAscIdAsistenciaAsc(Integer idEvento);

    long countByInscripcionEventoIdEvento(Integer idEvento);
}
