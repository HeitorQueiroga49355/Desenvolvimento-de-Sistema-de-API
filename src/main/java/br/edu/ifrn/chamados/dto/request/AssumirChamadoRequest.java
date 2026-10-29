package br.edu.ifrn.chamados.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssumirChamadoRequest(
    @NotNull(message = "O tecnicoId é obrigatório")
    Long tecnicoId
) {}
