package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.enums.PrioridadeChamado;
import br.edu.ifrn.chamados.model.enums.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

/** Contrato de acesso a dados de Chamado (implementado com JDBC em {@link JdbcChamadoRepository}). */
public interface ChamadoRepository {

    Optional<Chamado> findById(Long id);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByCategoriaSetorId(Long setorId);

    Page<Chamado> buscarComFiltros(
            StatusChamado status,
            PrioridadeChamado prioridade,
            Long setorId,
            Long categoriaId,
            Long solicitanteId,
            Long tecnicoId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            Pageable pageable
    );

    Page<Chamado> buscarPorUsuarioEStatus(Long usuarioId, StatusChamado status, Pageable pageable);

    Chamado save(Chamado chamado);
}
