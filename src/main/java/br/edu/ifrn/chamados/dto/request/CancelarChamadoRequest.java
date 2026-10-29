package br.edu.ifrn.chamados.dto.request;

import jakarta.validation.constraints.NotNull;

public record CancelarChamadoRequest(
    @NotNull(message = "O solicitanteId é obrigatório")
    Long solicitanteId
) {}
