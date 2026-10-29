package br.edu.ifrn.chamados.dto.response;

import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.enums.PrioridadeChamado;
import br.edu.ifrn.chamados.model.enums.StatusChamado;

import java.time.LocalDateTime;
import java.util.List;

public record ChamadoDetalheResponse(
    Long id,
    String titulo,
    String descricao,
    String local,
    PrioridadeChamado prioridade,
    StatusChamado status,
    LocalDateTime dataAbertura,
    LocalDateTime dataFechamento,
    String solucao,
    Long solicitanteId,
    String solicitanteNome,
    Long categoriaId,
    String categoriaNome,
    Long setorId,
    String setorSigla,
    Long tecnicoId,
    String tecnicoNome,
    List<InteracaoResponse> interacoes
) {
    public static ChamadoDetalheResponse from(Chamado chamado) {
        return new ChamadoDetalheResponse(
            chamado.getId(),
            chamado.getTitulo(),
            chamado.getDescricao(),
            chamado.getLocalizacao(),
            chamado.getPrioridade(),
            chamado.getStatus(),
            chamado.getDataAbertura(),
            chamado.getDataFechamento(),
            chamado.getSolucao(),
            chamado.getSolicitante().getId(),
            chamado.getSolicitante().getNome(),
            chamado.getCategoria().getId(),
            chamado.getCategoria().getNome(),
            chamado.getCategoria().getSetor().getId(),
            chamado.getCategoria().getSetor().getSigla(),
            chamado.getTecnico() != null ? chamado.getTecnico().getId() : null,
            chamado.getTecnico() != null ? chamado.getTecnico().getNome() : null,
            chamado.getInteracoes() != null
                ? chamado.getInteracoes().stream().map(InteracaoResponse::from).toList()
                : List.of()
        );
    }
}
