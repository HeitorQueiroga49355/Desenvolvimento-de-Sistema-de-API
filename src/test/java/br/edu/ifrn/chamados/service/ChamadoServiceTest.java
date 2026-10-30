package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.request.AssumirChamadoRequest;
import br.edu.ifrn.chamados.dto.request.CancelarChamadoRequest;
import br.edu.ifrn.chamados.dto.request.ChamadoCreateRequest;
import br.edu.ifrn.chamados.dto.request.InteracaoCreateRequest;
import br.edu.ifrn.chamados.dto.request.ResolverChamadoRequest;
import br.edu.ifrn.chamados.dto.response.ChamadoResponse;
import br.edu.ifrn.chamados.dto.response.InteracaoResponse;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Interacao;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.model.enums.PrioridadeChamado;
import br.edu.ifrn.chamados.model.enums.StatusChamado;
import br.edu.ifrn.chamados.model.enums.TipoUsuario;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.InteracaoRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private InteracaoRepository interacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ChamadoService chamadoService;

    private Setor setorTi;
    private Setor setorManutencao;
    private Categoria categoriaRede;
    private Usuario solicitante;
    private Usuario outroSolicitante;
    private Usuario tecnicoTi;
    private Usuario tecnicoManutencao;

    @BeforeEach
    void setUp() {
        setorTi = new Setor(1L, "TI", "COTIC");
        setorManutencao = new Setor(2L, "Manutenção", "COAS");

        categoriaRede = new Categoria(1L, "Rede e Wi-Fi", "Problemas de conexão", setorTi);

        solicitante = new Usuario(1L, "Alice", "alice@ifrn.edu.br", "2026101", TipoUsuario.SOLICITANTE, null);
        outroSolicitante = new Usuario(2L, "Bruno", "bruno@ifrn.edu.br", "2026102", TipoUsuario.SOLICITANTE, null);

        tecnicoTi = new Usuario(3L, "Carlos TI", "carlos@ifrn.edu.br", "2026201", TipoUsuario.TECNICO, setorTi);
        tecnicoManutencao = new Usuario(4L, "Daniel Manut", "daniel@ifrn.edu.br", "2026202", TipoUsuario.TECNICO, setorManutencao);
    }

    @Test
    void regra1E2_deveCriarChamadoComStatusAbertoEPrioridadeMediaPadrao() {
        ChamadoCreateRequest request = new ChamadoCreateRequest(
                "Sem internet",
                "Wi-Fi caiu na sala 10",
                "Sala 10",
                null,
                1L,
                1L
        );

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaRede));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> {
            Chamado c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        ChamadoResponse response = chamadoService.criar(request);

        assertNotNull(response);
        assertEquals(StatusChamado.ABERTO, response.status());
        assertEquals(PrioridadeChamado.MEDIA, response.prioridade());
        assertNotNull(response.dataAbertura());
    }

    @Test
    void regra2_deveLancarExcecaoQuandoTecnicoTentarAbrirChamado() {
        ChamadoCreateRequest request = new ChamadoCreateRequest(
                "Sem internet",
                "Wi-Fi caiu",
                "Sala 10",
                PrioridadeChamado.ALTA,
                3L,
                1L
        );

        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(tecnicoTi));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> chamadoService.criar(request));
        assertTrue(ex.getMessage().contains("SOLICITANTE"));
    }

    @Test
    void regra3_devePermitirTecnicoDoMesmoSetorAssumirChamadoAberto() {
        Chamado chamado = new Chamado(10L, "Título", "Desc", "Lab 1",
                PrioridadeChamado.MEDIA, StatusChamado.ABERTO, LocalDateTime.now(), null, null, solicitante, categoriaRede, null);

        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(tecnicoTi));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(i -> i.getArgument(0));

        ChamadoResponse response = chamadoService.assumir(10L, new AssumirChamadoRequest(3L));

        assertEquals(StatusChamado.EM_ATENDIMENTO, response.status());
        assertEquals(3L, response.tecnicoId());
    }

    @Test
    void regra3_deveLancarExcecaoQuandoTecnicoDeOutroSetorTentarAssumir() {
        Chamado chamado = new Chamado(10L, "Título", "Desc", "Lab 1",
                PrioridadeChamado.MEDIA, StatusChamado.ABERTO, LocalDateTime.now(), null, null, solicitante, categoriaRede, null);

        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(tecnicoManutencao));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> chamadoService.assumir(10L, new AssumirChamadoRequest(4L)));
        assertTrue(ex.getMessage().contains("mesmo setor"));
    }

    @Test
    void regra4_devePermitirApenasTecnicoAtribuidoResolverChamadoComSolucao() {
        Chamado chamado = new Chamado(10L, "Título", "Desc", "Lab 1",
                PrioridadeChamado.MEDIA, StatusChamado.EM_ATENDIMENTO, LocalDateTime.now(), null, null, solicitante, categoriaRede, tecnicoTi);

        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(tecnicoTi));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(i -> i.getArgument(0));

        ChamadoResponse response = chamadoService.resolver(10L, new ResolverChamadoRequest(3L, "Roteador reiniciado"));

        assertEquals(StatusChamado.RESOLVIDO, response.status());
        assertEquals("Roteador reiniciado", response.solucao());
        assertNotNull(response.dataFechamento());
    }

    @Test
    void regra4_deveLancarExcecaoSeOutroTecnicoTentarResolver() {
        Usuario outroTecnicoTi = new Usuario(5L, "Outro TI", "outro@ifrn.edu.br", "2026203", TipoUsuario.TECNICO, setorTi);
        Chamado chamado = new Chamado(10L, "Título", "Desc", "Lab 1",
                PrioridadeChamado.MEDIA, StatusChamado.EM_ATENDIMENTO, LocalDateTime.now(), null, null, solicitante, categoriaRede, tecnicoTi);

        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(outroTecnicoTi));

        assertThrows(RegraNegocioException.class,
                () -> chamadoService.resolver(10L, new ResolverChamadoRequest(5L, "Solução")));
    }

    @Test
    void regra5_devePermitirApenasProprioSolicitanteCancelarChamadoAberto() {
        Chamado chamado = new Chamado(10L, "Título", "Desc", "Lab 1",
                PrioridadeChamado.MEDIA, StatusChamado.ABERTO, LocalDateTime.now(), null, null, solicitante, categoriaRede, null);

        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(i -> i.getArgument(0));

        ChamadoResponse response = chamadoService.cancelar(10L, new CancelarChamadoRequest(1L));

        assertEquals(StatusChamado.CANCELADO, response.status());
    }

    @Test
    void regra5_deveLancarExcecaoSeOutroUsuarioTentarCancelarOuStatusNaoAberto() {
        Chamado chamado = new Chamado(10L, "Título", "Desc", "Lab 1",
                PrioridadeChamado.MEDIA, StatusChamado.EM_ATENDIMENTO, LocalDateTime.now(), null, null, solicitante, categoriaRede, tecnicoTi);

        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));

        assertThrows(RegraNegocioException.class,
                () -> chamadoService.cancelar(10L, new CancelarChamadoRequest(1L)));
    }

    @Test
    void regra6_deveLancarExcecaoSeTentarInteragirEmChamadoResolvidoOuCancelado() {
        Chamado chamado = new Chamado(10L, "Título", "Desc", "Lab 1",
                PrioridadeChamado.MEDIA, StatusChamado.RESOLVIDO, LocalDateTime.now(), LocalDateTime.now(), "Ok", solicitante, categoriaRede, tecnicoTi);

        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));

        assertThrows(RegraNegocioException.class,
                () -> chamadoService.adicionarInteracao(10L, new InteracaoCreateRequest(1L, "Comentário")));
    }

    @Test
    void regra7_devePermitirSolicitanteETecnicoRegistraremInteracao() {
        Chamado chamado = new Chamado(10L, "Título", "Desc", "Lab 1",
                PrioridadeChamado.MEDIA, StatusChamado.EM_ATENDIMENTO, LocalDateTime.now(), null, null, solicitante, categoriaRede, tecnicoTi);

        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
        when(interacaoRepository.save(any(Interacao.class))).thenAnswer(i -> {
            Interacao it = i.getArgument(0);
            it.setId(100L);
            return it;
        });

        InteracaoResponse response = chamadoService.adicionarInteracao(10L, new InteracaoCreateRequest(1L, "Ainda está com problema"));

        assertNotNull(response);
        assertEquals("Ainda está com problema", response.texto());
        assertEquals(1L, response.autorId());
    }

    @Test
    void regra7_deveLancarExcecaoQuandoTerceiroTentarRegistrarInteracao() {
        Chamado chamado = new Chamado(10L, "Título", "Desc", "Lab 1",
                PrioridadeChamado.MEDIA, StatusChamado.EM_ATENDIMENTO, LocalDateTime.now(), null, null, solicitante, categoriaRede, tecnicoTi);

        when(chamadoRepository.findById(10L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(outroSolicitante));

        assertThrows(RegraNegocioException.class,
                () -> chamadoService.adicionarInteracao(10L, new InteracaoCreateRequest(2L, "Tentando comentar")));
    }
}
