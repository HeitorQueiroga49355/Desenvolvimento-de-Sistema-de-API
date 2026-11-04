package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/** Contrato de acesso a dados de Categoria (implementado com JDBC em {@link JdbcCategoriaRepository}). */
public interface CategoriaRepository {

    Optional<Categoria> findById(Long id);

    List<Categoria> findBySetorId(Long setorId);

    Page<Categoria> findBySetorId(Long setorId, Pageable pageable);

    Page<Categoria> buscarComFiltros(String nome, Long setorId, Pageable pageable);

    Categoria save(Categoria categoria);

    void delete(Categoria categoria);
}
