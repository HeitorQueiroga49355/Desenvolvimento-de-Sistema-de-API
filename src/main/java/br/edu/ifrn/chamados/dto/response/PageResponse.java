package br.edu.ifrn.chamados.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
    List<T> itens,
    int numeroPagina,
    int tamanho,
    long totalElementos,
    int totalPaginas
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
