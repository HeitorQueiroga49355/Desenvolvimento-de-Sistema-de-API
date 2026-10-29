package br.edu.ifrn.chamados.dto.response;

import br.edu.ifrn.chamados.model.Categoria;

public record CategoriaResponse(
    Long id,
    String nome,
    String descricao,
    Long setorId,
    String setorSigla
) {
    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(
            categoria.getId(),
            categoria.getNome(),
            categoria.getDescricao(),
            categoria.getSetor().getId(),
            categoria.getSetor().getSigla()
        );
    }
}
