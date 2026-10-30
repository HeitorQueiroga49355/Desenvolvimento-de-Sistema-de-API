package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.request.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.request.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.request.ChamadoCreateRequest;
import br.edu.ifrn.chamados.dto.request.InteracaoCreateRequest;
import br.edu.ifrn.chamados.dto.request.ResolverChamadoRequest;
import br.edu.ifrn.chamados.dto.response.ChamadoDetalheResponse;
import br.edu.ifrn.chamados.dto.response.ChamadoResponse;
import br.edu.ifrn.chamados.dto.response.InteracaoResponse;
import br.edu.ifrn.chamados.dto.response.PageResponse;
import br.edu.ifrn.chamados.model.enums.PrioridadeChamado;
import br.edu.ifrn.chamados.model.enums.StatusChamado;
import br.edu.ifrn.chamados.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> criar(@Valid @RequestBody ChamadoCreateRequest request) {
        ChamadoResponse response = chamadoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ChamadoResponse>> listar(
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) PrioridadeChamado prioridade,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long solicitanteId,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(chamadoService.listar(
                status,
                prioridade,
                setorId,
                categoriaId,
                solicitanteId,
                tecnicoId,
                dataInicio,
                dataFim,
                pageable
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChamadoDetalheResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(chamadoService.buscarPorId(id));
    }

    @PatchMapping("/{id}/assumir")
    public ResponseEntity<ChamadoResponse> assumir(
            @PathVariable Long id,
            @Valid @RequestBody AssumirChamadoRequest request
    ) {
        return ResponseEntity.ok(chamadoService.assumir(id, request));
    }

    @PatchMapping("/{id}/resolver")
    public ResponseEntity<ChamadoResponse> resolver(
            @PathVariable Long id,
            @Valid @RequestBody ResolverChamadoRequest request
    ) {
        return ResponseEntity.ok(chamadoService.resolver(id, request));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ChamadoResponse> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelarChamadoRequest request
    ) {
        return ResponseEntity.ok(chamadoService.cancelar(id, request));
    }

    @PostMapping("/{id}/interacoes")
    public ResponseEntity<InteracaoResponse> adicionarInteracao(
            @PathVariable Long id,
            @Valid @RequestBody InteracaoCreateRequest request
    ) {
        InteracaoResponse response = chamadoService.adicionarInteracao(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
