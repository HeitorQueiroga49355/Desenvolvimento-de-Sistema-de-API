package br.edu.ifrn.chamados.dto.request;

import br.edu.ifrn.chamados.model.enums.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    String nome,

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Formato de email inválido")
    @Size(max = 150, message = "O email deve ter no máximo 150 caracteres")
    String email,

    @NotBlank(message = "A matrícula é obrigatória")
    @Size(max = 50, message = "A matrícula deve ter no máximo 50 caracteres")
    String matricula,

    @NotNull(message = "O tipo de usuário é obrigatório")
    TipoUsuario tipo,

    Long setorId
) {}
