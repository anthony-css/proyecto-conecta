package com.cibertec.conecta.service;

import com.cibertec.conecta.dto.categoria.CategoriaRequest;
import com.cibertec.conecta.dto.categoria.CategoriaResponse;
import com.cibertec.conecta.exception.ConflictoException;
import com.cibertec.conecta.exception.RecursoNoEncontradoException;
import com.cibertec.conecta.model.Categoria;
import com.cibertec.conecta.repository.CategoriaRepository;
import com.cibertec.conecta.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final EventoRepository eventoRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(boolean soloActivas) {
        List<Categoria> categorias = soloActivas
                ? categoriaRepository.findByActivoTrueOrderByNombreAsc()
                : categoriaRepository.findAllByOrderByNombreAsc();

        return categorias.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Integer id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.nombre().trim())) {
            throw new ConflictoException("Ya existe una categoria con ese nombre", "CATEGORIA_DUPLICADA");
        }

        Categoria categoria = new Categoria();
        aplicar(categoria, request);
        guardar(categoria);
        return toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse actualizar(Integer id, CategoriaRequest request) {
        Categoria categoria = buscar(id);

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(request.nombre().trim(), id)) {
            throw new ConflictoException("Ya existe una categoria con ese nombre", "CATEGORIA_DUPLICADA");
        }

        aplicar(categoria, request);
        guardar(categoria);
        return toResponse(categoria);
    }

    @Transactional
    public void eliminar(Integer id) {
        Categoria categoria = buscar(id);

        if (eventoRepository.existsByCategoriaIdCategoria(id)) {
            // Baja logica: conserva el historial de eventos.
            categoria.setActivo(false);
            categoriaRepository.save(categoria);
            return;
        }

        categoriaRepository.delete(categoria);
    }

    private Categoria buscar(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria no encontrada"));
    }

    private void aplicar(Categoria categoria, CategoriaRequest request) {
        categoria.setNombre(request.nombre().trim());
        categoria.setDescripcion(request.descripcion());
        categoria.setColor(request.color());
        categoria.setIcono(request.icono());
        if (request.activo() != null) {
            categoria.setActivo(request.activo());
        }
    }

    private void guardar(Categoria categoria) {
        try {
            categoriaRepository.saveAndFlush(categoria);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException constraint) {
                String nombre = constraint.getConstraintName();
                if (nombre != null && (nombre.equals("uq_categoria_nombre")
                        || nombre.endsWith(".uq_categoria_nombre"))) {
                    throw new ConflictoException("Ya existe una categoria con ese nombre", "CATEGORIA_DUPLICADA");
                }
            }
            throw ex;
        }
    }

    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(
                c.getIdCategoria(),
                c.getNombre(),
                c.getDescripcion(),
                c.getColor(),
                c.getIcono(),
                c.getActivo()
        );
    }
}
