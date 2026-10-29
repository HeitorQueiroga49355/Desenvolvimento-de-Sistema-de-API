package br.edu.ifrn.chamados.dto.response;

import java.time.LocalDateTime;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String mensagem,
    String caminho
) {}
