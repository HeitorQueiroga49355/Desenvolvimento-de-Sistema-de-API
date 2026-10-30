package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.request.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.request.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.request.ChamadoCreateRequest;
import br.edu.ifrn.chamados.dto.request.InteracaoCreateRequest;
import br.edu.ifrn.chamados.dto.request.ResolverChamadoRequest;
import br.edu.ifrn.chamados.dto.response.ChamadoDetalheResponse;
import br.edu.ifrn.chamados.dto.response.ChamadoResponse;
import br.edu.ifrn.chamados.dto.response.InteracaoResponse;
import br.edu.ifrn.chamados.dto.response.PageResponse;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Interacao;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.model.enums.PrioridadeChamado;
import br.edu.ifrn.chamados.model.enums.StatusChamado;
import br.edu.ifrn.chamados.model.enums.TipoUsuario;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.InteracaoRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final InteracaoRepository interacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            InteracaoRepository interacaoRepository,
            UsuarioRepository usuarioRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.chamadoRepository = chamadoRepository;
        this.interacaoRepository = interacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoCreateRequest request) {
        Usuario solicitante = buscarUsuarioPorId(request.solicitanteId());

        if (solicitante.getTipo() != TipoUsuario.SOLICITANTE) {
            throw new RegraNegocioException("Apenas usuários do tipo SOLICITANTE podem abrir chamados.");
        }

        Categoria categoria = buscarCategoriaPorId(request.categoriaId());

        PrioridadeChamado prioridade = request.prioridade() != null
                ? request.prioridade()
                : PrioridadeChamado.MEDIA;

        Chamado chamado = new Chamado(
                null,
                request.titulo(),
                request.descricao(),
                request.local(),
                prioridade,
                StatusChamado.ABERTO,
                LocalDateTime.now(),
                null,
                null,
                solicitante,
                categoria,
                null
        );

        return ChamadoResponse.from(chamadoRepository.save(chamado));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChamadoResponse> listar(
            StatusChamado status,
            PrioridadeChamado prioridade,
            Long setorId,
            Long categoriaId,
            Long solicitanteId,
            Long tecnicoId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            Pageable pageable
    ) {
        Page<Chamado> page = chamadoRepository.buscarComFiltros(
                status,
                prioridade,
                setorId,
                categoriaId,
                solicitanteId,
                tecnicoId,
                dataInicio,
                dataFim,
                pageable
        );
        return PageResponse.from(page.map(ChamadoResponse::from));
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscarPorId(Long id) {
        Chamado chamado = buscarEntidadePorId(id);
        return ChamadoDetalheResponse.from(chamado);
    }

    @Transactional
    public ChamadoResponse assumir(Long id, AssumirChamadoRequest request) {
        Chamado chamado = buscarEntidadePorId(id);
        Usuario tecnico = buscarUsuarioPorId(request.tecnicoId());

        if (tecnico.getTipo() != TipoUsuario.TECNICO) {
            throw new RegraNegocioException("O usuário informado não é um técnico.");
        }

        if (chamado.getStatus() != StatusChamado.ABERTO) {
            throw new RegraNegocioException("Apenas chamados com status ABERTO podem ser assumidos.");
        }

        Long setorTecnicoId = tecnico.getSetor() != null ? tecnico.getSetor().getId() : null;
        Long setorChamadoId = chamado.getCategoria().getSetor().getId();

        if (setorTecnicoId == null || !setorTecnicoId.equals(setorChamadoId)) {
            throw new RegraNegocioException("O técnico deve pertencer ao mesmo setor da categoria do chamado.");
        }

        chamado.setTecnico(tecnico);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);

        return ChamadoResponse.from(chamadoRepository.save(chamado));
    }

    @Transactional
    public ChamadoResponse resolver(Long id, ResolverChamadoRequest request) {
        Chamado chamado = buscarEntidadePorId(id);
        Usuario tecnico = buscarUsuarioPorId(request.tecnicoId());

        if (chamado.getStatus() != StatusChamado.EM_ATENDIMENTO) {
            throw new RegraNegocioException("Apenas chamados com status EM_ATENDIMENTO podem ser resolvidos.");
        }

        if (chamado.getTecnico() == null || !chamado.getTecnico().getId().equals(tecnico.getId())) {
            throw new RegraNegocioException("Somente o técnico atribuído ao chamado pode resolvê-lo.");
        }

        if (request.solucao() == null || request.solucao().trim().isEmpty()) {
            throw new RegraNegocioException("A solução é obrigatória para a resolução do chamado.");
        }

        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setSolucao(request.solucao().trim());
        chamado.setDataFechamento(LocalDateTime.now());

        return ChamadoResponse.from(chamadoRepository.save(chamado));
    }

    @Transactional
    public ChamadoResponse cancelar(Long id, CancelarChamadoRequest request) {
        Chamado chamado = buscarEntidadePorId(id);
        Usuario solicitante = buscarUsuarioPorId(request.solicitanteId());

        if (chamado.getStatus() != StatusChamado.ABERTO) {
            throw new RegraNegocioException("Apenas chamados com status ABERTO podem ser cancelados.");
        }

        if (!chamado.getSolicitante().getId().equals(solicitante.getId())) {
            throw new RegraNegocioException("Somente o próprio solicitante pode cancelar o chamado.");
        }

        chamado.setStatus(StatusChamado.CANCELADO);

        return ChamadoResponse.from(chamadoRepository.save(chamado));
    }

    @Transactional
    public InteracaoResponse adicionarInteracao(Long id, InteracaoCreateRequest request) {
        Chamado chamado = buscarEntidadePorId(id);

        if (chamado.getStatus() == StatusChamado.RESOLVIDO || chamado.getStatus() == StatusChamado.CANCELADO) {
            throw new RegraNegocioException("Chamados resolvidos ou cancelados não aceitam novas interações.");
        }

        Usuario autor = buscarUsuarioPorId(request.autorId());

        boolean isSolicitante = chamado.getSolicitante().getId().equals(autor.getId());
        boolean isTecnicoAtribuido = chamado.getTecnico() != null && chamado.getTecnico().getId().equals(autor.getId());

        if (!isSolicitante && !isTecnicoAtribuido) {
            throw new RegraNegocioException("Apenas o solicitante e o técnico atribuído podem registrar interações no chamado.");
        }

        Interacao interacao = new Interacao(
                null,
                request.texto().trim(),
                LocalDateTime.now(),
                chamado,
                autor
        );

        return InteracaoResponse.from(interacaoRepository.save(interacao));
    }

    @Transactional(readOnly = true)
    public Chamado buscarEntidadePorId(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado com o ID: " + id));
    }

    private Usuario buscarUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID: " + id));
    }

    private Categoria buscarCategoriaPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada com o ID: " + id));
    }
}
