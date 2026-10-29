package br.edu.ifrn.chamados.dto.response;

import br.edu.ifrn.chamados.model.Interacao;

import java.time.LocalDateTime;

public record InteracaoResponse(
    Long id,
    String texto,
    LocalDateTime dataHora,
    Long autorId,
    String autorNome
) {
    public static InteracaoResponse from(Interacao interacao) {
        return new InteracaoResponse(
            interacao.getId(),
            interacao.getTexto(),
            interacao.getDataHora(),
            interacao.getAutor().getId(),
            interacao.getAutor().getNome()
        );
    }
}
