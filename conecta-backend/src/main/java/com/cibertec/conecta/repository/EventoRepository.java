package com.cibertec.conecta.repository;

import com.cibertec.conecta.model.Evento;
import com.cibertec.conecta.model.enums.EstadoEvento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evento e where e.idEvento = :id")
    Optional<Evento> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByCategoriaIdCategoria(Integer idCategoria);

    long countByEstado(EstadoEvento estado);

    long countByEstadoIn(Collection<EstadoEvento> estados);

    List<Evento> findTop5ByEstadoOrderByFechaAscHoraInicioAsc(EstadoEvento estado);

    long countByFechaGreaterThanEqual(LocalDate fecha);

    @Query("""
            select count(e) from Evento e
            where e.estado in :estados
              and (e.fecha > :fecha or (e.fecha = :fecha and e.horaInicio > :hora))
            """)
    long contarProximos(@Param("estados") Collection<EstadoEvento> estados, @Param("fecha") LocalDate fecha,
                        @Param("hora") java.time.LocalTime hora);

    @Query("""
            select e from Evento e join fetch e.categoria
            where e.destacado = true and e.estado in :estados
              and (e.fecha > :fecha or (e.fecha = :fecha and e.horaInicio > :hora))
            order by e.fecha asc, e.horaInicio asc, e.idEvento asc
            """)
    List<Evento> buscarDestacados(@Param("estados") Collection<EstadoEvento> estados,
                                  @Param("fecha") LocalDate fecha,
                                  @Param("hora") java.time.LocalTime hora,
                                  Pageable pageable);

    @Query("""
            select e from Evento e
            join fetch e.categoria c
            where (:estado is null or e.estado = :estado)
              and (:idCategoria is null or c.idCategoria = :idCategoria)
              and (:q is null or lower(e.titulo) like lower(concat('%', :q, '%')))
              and (:fecha is null or e.fecha = :fecha)
            """)
    Page<Evento> buscar(@Param("estado") EstadoEvento estado,
                        @Param("idCategoria") Integer idCategoria,
                        @Param("q") String q,
                        @Param("fecha") LocalDate fecha,
                        Pageable pageable);

    @Query("""
            select e from Evento e
            join fetch e.categoria c
            where e.idEvento = :id
            """)
    Optional<Evento> findByIdConCategoria(@Param("id") Integer id);

    @Query("""
            select e from Evento e
            join fetch e.categoria c
            where e.estado = :estado
            order by e.fecha asc, e.horaInicio asc
            """)
    List<Evento> listarPorEstadoConCategoria(@Param("estado") EstadoEvento estado);

    @Query(value = """
            select e from Evento e
            join fetch e.categoria c
            where e.estado in :estados
              and (:idCategoria is null or c.idCategoria = :idCategoria)
              and (:q is null or lower(e.titulo) like lower(concat('%', :q, '%')))
              and (:fecha is null or e.fecha = :fecha)
              and (:hoy is null or e.fecha > :hoy or (e.fecha = :hoy and e.horaFin > :hora))
            """,
            countQuery = """
            select count(e) from Evento e
            join e.categoria c
            where e.estado in :estados
              and (:idCategoria is null or c.idCategoria = :idCategoria)
              and (:q is null or lower(e.titulo) like lower(concat('%', :q, '%')))
              and (:fecha is null or e.fecha = :fecha)
              and (:hoy is null or e.fecha > :hoy or (e.fecha = :hoy and e.horaFin > :hora))
            """)
    Page<Evento> buscarPorEstados(@Param("estados") Collection<EstadoEvento> estados,
                                  @Param("idCategoria") Integer idCategoria,
                                  @Param("q") String q,
                                  @Param("fecha") LocalDate fecha,
                                  @Param("hoy") LocalDate hoy,
                                  @Param("hora") java.time.LocalTime hora,
                                  Pageable pageable);
}
