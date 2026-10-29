package br.edu.ifrn.chamados.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolverChamadoRequest(
    @NotNull(message = "O tecnicoId é obrigatório")
    Long tecnicoId,

    @NotBlank(message = "A solução é obrigatória")
    String solucao
) {}
