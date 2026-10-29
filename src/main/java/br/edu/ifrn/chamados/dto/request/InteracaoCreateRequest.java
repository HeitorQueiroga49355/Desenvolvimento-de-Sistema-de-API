package br.edu.ifrn.chamados.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InteracaoCreateRequest(
    @NotNull(message = "O autorId é obrigatório")
    Long autorId,

    @NotBlank(message = "O texto é obrigatório")
    String texto
) {}
