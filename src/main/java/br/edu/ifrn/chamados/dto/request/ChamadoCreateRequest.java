package br.edu.ifrn.chamados.dto.request;

import br.edu.ifrn.chamados.model.enums.PrioridadeChamado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoCreateRequest(
    @NotBlank(message = "O título é obrigatório")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
    String titulo,

    @NotBlank(message = "A descrição é obrigatória")
    String descricao,

    @NotBlank(message = "O local é obrigatório")
    @Size(max = 100, message = "O local deve ter no máximo 100 caracteres")
    String local,

    PrioridadeChamado prioridade,

    @NotNull(message = "O solicitanteId é obrigatório")
    Long solicitanteId,

    @NotNull(message = "O categoriaId é obrigatório")
    Long categoriaId
) {}
