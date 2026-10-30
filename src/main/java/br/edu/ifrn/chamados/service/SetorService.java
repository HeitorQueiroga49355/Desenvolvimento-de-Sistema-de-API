package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.request.SetorRequest;
import br.edu.ifrn.chamados.dto.response.CategoriaResponse;
import br.edu.ifrn.chamados.dto.response.PageResponse;
import br.edu.ifrn.chamados.dto.response.SetorResponse;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetorService {

    private final SetorRepository setorRepository;
    private final CategoriaRepository categoriaRepository;
    private final ChamadoRepository chamadoRepository;

    public SetorService(
            SetorRepository setorRepository,
            CategoriaRepository categoriaRepository,
            ChamadoRepository chamadoRepository
    ) {
        this.setorRepository = setorRepository;
        this.categoriaRepository = categoriaRepository;
        this.chamadoRepository = chamadoRepository;
    }

    @Transactional
    public SetorResponse criar(SetorRequest request) {
        if (setorRepository.existsBySiglaIgnoreCase(request.sigla())) {
            throw new RegraNegocioException("Já existe um setor cadastrado com a sigla: " + request.sigla());
        }

        Setor setor = new Setor(null, request.nome(), request.sigla());
        return SetorResponse.from(setorRepository.save(setor));
    }

    @Transactional(readOnly = true)
    public PageResponse<SetorResponse> listar(String nome, Pageable pageable) {
        Page<Setor> page = setorRepository.buscarComFiltros(nome, pageable);
        return PageResponse.from(page.map(SetorResponse::from));
    }

    @Transactional(readOnly = true)
    public SetorResponse buscarPorId(Long id) {
        Setor setor = buscarEntidadePorId(id);
        return SetorResponse.from(setor);
    }

    @Transactional
    public SetorResponse atualizar(Long id, SetorRequest request) {
        Setor setor = buscarEntidadePorId(id);

        if (setorRepository.existsBySiglaIgnoreCaseAndIdNot(request.sigla(), id)) {
            throw new RegraNegocioException("Já existe outro setor com a sigla: " + request.sigla());
        }

        setor.setNome(request.nome());
        setor.setSigla(request.sigla());

        return SetorResponse.from(setorRepository.save(setor));
    }

    @Transactional
    public void excluir(Long id) {
        Setor setor = buscarEntidadePorId(id);

        if (chamadoRepository.existsByCategoriaSetorId(id)) {
            throw new RegraNegocioException("Não é permitido excluir setor que possui chamados vinculados.");
        }

        setorRepository.delete(setor);
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoriaResponse> listarCategorias(Long setorId, Pageable pageable) {
        buscarEntidadePorId(setorId);
        Page<CategoriaResponse> page = categoriaRepository.findBySetorId(setorId, pageable)
                .map(CategoriaResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public Setor buscarEntidadePorId(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado com o ID: " + id));
    }
}
