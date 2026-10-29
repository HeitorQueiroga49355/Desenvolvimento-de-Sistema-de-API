package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.model.enums.TipoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("SELECT u FROM Usuario u WHERE " +
           "(:nome IS NULL OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "(:tipo IS NULL OR u.tipo = :tipo) AND " +
           "(:setorId IS NULL OR u.setor.id = :setorId)")
    Page<Usuario> buscarComFiltros(
            @Param("nome") String nome,
            @Param("tipo") TipoUsuario tipo,
            @Param("setorId") Long setorId,
            Pageable pageable
    );
}
