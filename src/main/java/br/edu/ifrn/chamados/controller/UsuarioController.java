package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.request.UsuarioRequest;
import br.edu.ifrn.chamados.dto.response.ChamadoResponse;
import br.edu.ifrn.chamados.dto.response.PageResponse;
import br.edu.ifrn.chamados.dto.response.UsuarioResponse;
import br.edu.ifrn.chamados.model.enums.StatusChamado;
import br.edu.ifrn.chamados.model.enums.TipoUsuario;
import br.edu.ifrn.chamados.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse response = usuarioService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) TipoUsuario tipo,
            @RequestParam(required = false) Long setorId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(usuarioService.listar(nome, tipo, setorId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequest request
    ) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @GetMapping("/{id}/chamados")
    public ResponseEntity<PageResponse<ChamadoResponse>> buscarChamadosDoUsuario(
            @PathVariable Long id,
            @RequestParam(required = false) StatusChamado status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(usuarioService.buscarChamadosDoUsuario(id, status, pageable));
    }
}
