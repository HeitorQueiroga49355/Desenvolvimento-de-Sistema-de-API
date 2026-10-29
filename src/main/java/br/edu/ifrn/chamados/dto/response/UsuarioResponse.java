package br.edu.ifrn.chamados.dto.response;

import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.model.enums.TipoUsuario;

public record UsuarioResponse(
    Long id,
    String nome,
    String email,
    String matricula,
    TipoUsuario tipo,
    Long setorId,
    String setorSigla
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getMatricula(),
            usuario.getTipo(),
            usuario.getSetor() != null ? usuario.getSetor().getId() : null,
            usuario.getSetor() != null ? usuario.getSetor().getSigla() : null
        );
    }
}
