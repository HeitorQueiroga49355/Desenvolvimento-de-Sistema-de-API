package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.enums.PrioridadeChamado;
import br.edu.ifrn.chamados.model.enums.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByCategoriaSetorId(Long setorId);

    @Query("SELECT c FROM Chamado c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:prioridade IS NULL OR c.prioridade = :prioridade) AND " +
           "(:setorId IS NULL OR c.categoria.setor.id = :setorId) AND " +
           "(:categoriaId IS NULL OR c.categoria.id = :categoriaId) AND " +
           "(:solicitanteId IS NULL OR c.solicitante.id = :solicitanteId) AND " +
           "(:tecnicoId IS NULL OR (c.tecnico IS NOT NULL AND c.tecnico.id = :tecnicoId)) AND " +
           "(:dataInicio IS NULL OR c.dataAbertura >= :dataInicio) AND " +
           "(:dataFim IS NULL OR c.dataAbertura <= :dataFim)")
    Page<Chamado> buscarComFiltros(
            @Param("status") StatusChamado status,
            @Param("prioridade") PrioridadeChamado prioridade,
            @Param("setorId") Long setorId,
            @Param("categoriaId") Long categoriaId,
            @Param("solicitanteId") Long solicitanteId,
            @Param("tecnicoId") Long tecnicoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable
    );

    @Query("SELECT c FROM Chamado c WHERE " +
           "(c.solicitante.id = :usuarioId OR (c.tecnico IS NOT NULL AND c.tecnico.id = :usuarioId)) AND " +
           "(:status IS NULL OR c.status = :status)")
    Page<Chamado> buscarPorUsuarioEStatus(
            @Param("usuarioId") Long usuarioId,
            @Param("status") StatusChamado status,
            Pageable pageable
    );
}
