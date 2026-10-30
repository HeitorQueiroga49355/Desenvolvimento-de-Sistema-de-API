package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.request.SetorRequest;
import br.edu.ifrn.chamados.dto.response.CategoriaResponse;
import br.edu.ifrn.chamados.dto.response.PageResponse;
import br.edu.ifrn.chamados.dto.response.SetorResponse;
import br.edu.ifrn.chamados.service.SetorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/setores")
public class SetorController {

    private final SetorService setorService;

    public SetorController(SetorService setorService) {
        this.setorService = setorService;
    }

    @PostMapping
    public ResponseEntity<SetorResponse> criar(@Valid @RequestBody SetorRequest request) {
        SetorResponse response = setorService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<SetorResponse>> listar(
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(setorService.listar(nome, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SetorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(setorService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SetorResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody SetorRequest request
    ) {
        return ResponseEntity.ok(setorService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        setorService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/categorias")
    public ResponseEntity<PageResponse<CategoriaResponse>> listarCategorias(
            @PathVariable Long id,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(setorService.listarCategorias(id, pageable));
    }
}
