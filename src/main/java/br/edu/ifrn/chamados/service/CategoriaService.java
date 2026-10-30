package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.request.CategoriaRequest;
import br.edu.ifrn.chamados.dto.response.CategoriaResponse;
import br.edu.ifrn.chamados.dto.response.PageResponse;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ChamadoRepository chamadoRepository;
    private final SetorRepository setorRepository;

    public CategoriaService(
            CategoriaRepository categoriaRepository,
            ChamadoRepository chamadoRepository,
            SetorRepository setorRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.chamadoRepository = chamadoRepository;
        this.setorRepository = setorRepository;
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        Setor setor = buscarSetorPorId(request.setorId());
        Categoria categoria = new Categoria(null, request.nome(), request.descricao(), setor);
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoriaResponse> listar(String nome, Long setorId, Pageable pageable) {
        Page<Categoria> page = categoriaRepository.buscarComFiltros(nome, setorId, pageable);
        return PageResponse.from(page.map(CategoriaResponse::from));
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        Categoria categoria = buscarEntidadePorId(id);
        return CategoriaResponse.from(categoria);
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarEntidadePorId(id);
        Setor setor = buscarSetorPorId(request.setorId());

        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());
        categoria.setSetor(setor);

        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarEntidadePorId(id);

        if (chamadoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Não é permitido excluir categoria que possui chamados vinculados.");
        }

        categoriaRepository.delete(categoria);
    }

    @Transactional(readOnly = true)
    public Categoria buscarEntidadePorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada com o ID: " + id));
    }

    private Setor buscarSetorPorId(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado com o ID: " + id));
    }
}
