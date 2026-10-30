package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.request.UsuarioRequest;
import br.edu.ifrn.chamados.dto.response.ChamadoResponse;
import br.edu.ifrn.chamados.dto.response.PageResponse;
import br.edu.ifrn.chamados.dto.response.UsuarioResponse;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.model.enums.StatusChamado;
import br.edu.ifrn.chamados.model.enums.TipoUsuario;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;
    private final SetorRepository setorRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            ChamadoRepository chamadoRepository,
            SetorRepository setorRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.chamadoRepository = chamadoRepository;
        this.setorRepository = setorRepository;
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com o e-mail: " + request.email());
        }

        Setor setor = resolverSetorParaUsuario(request.tipo(), request.setorId());

        Usuario usuario = new Usuario(
                null,
                request.nome(),
                request.email(),
                request.matricula(),
                request.tipo(),
                setor
        );

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String nome, TipoUsuario tipo, Long setorId, Pageable pageable) {
        Page<Usuario> page = usuarioRepository.buscarComFiltros(nome, tipo, setorId, pageable);
        return PageResponse.from(page.map(UsuarioResponse::from));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidadePorId(id);

        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new RegraNegocioException("Já existe outro usuário com o e-mail: " + request.email());
        }

        Setor setor = resolverSetorParaUsuario(request.tipo(), request.setorId());

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setMatricula(request.matricula());
        usuario.setTipo(request.tipo());
        usuario.setSetor(setor);

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChamadoResponse> buscarChamadosDoUsuario(Long usuarioId, StatusChamado status, Pageable pageable) {
        buscarEntidadePorId(usuarioId);
        Page<Chamado> page = chamadoRepository.buscarPorUsuarioEStatus(usuarioId, status, pageable);
        return PageResponse.from(page.map(ChamadoResponse::from));
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID: " + id));
    }

    private Setor resolverSetorParaUsuario(TipoUsuario tipo, Long setorId) {
        if (tipo == TipoUsuario.TECNICO) {
            if (setorId == null) {
                throw new RegraNegocioException("Usuário do tipo TÉCNICO deve pertencer a um setor.");
            }
            return buscarSetorPorId(setorId);
        }

        if (setorId != null) {
            return buscarSetorPorId(setorId);
        }

        return null;
    }

    private Setor buscarSetorPorId(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado com o ID: " + id));
    }
}
