package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findBySetorId(Long setorId);

    Page<Categoria> findBySetorId(Long setorId, Pageable pageable);

    @Query("SELECT c FROM Categoria c WHERE " +
           "(:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "(:setorId IS NULL OR c.setor.id = :setorId)")
    Page<Categoria> buscarComFiltros(
            @Param("nome") String nome,
            @Param("setorId") Long setorId,
            Pageable pageable
    );
}
