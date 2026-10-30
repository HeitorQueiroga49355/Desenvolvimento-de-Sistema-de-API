package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.request.SetorRequest;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetorECategoriaServiceTest {

    @Mock
    private SetorRepository setorRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ChamadoRepository chamadoRepository;

    @InjectMocks
    private SetorService setorService;

    @Test
    void regra8_deveLancarExcecaoAoExcluirSetorComChamados() {
        Setor setor = new Setor(1L, "TI", "COTIC");
        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor));
        when(chamadoRepository.existsByCategoriaSetorId(1L)).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> setorService.excluir(1L));
        assertTrue(ex.getMessage().contains("chamados vinculados"));
    }

    @Test
    void regra8_deveLancarExcecaoAoExcluirCategoriaComChamados() {
        CategoriaService categoriaService = new CategoriaService(categoriaRepository, chamadoRepository, setorRepository);
        Setor setor = new Setor(1L, "TI", "COTIC");
        Categoria categoria = new Categoria(1L, "Rede", "Redes", setor);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(chamadoRepository.existsByCategoriaId(1L)).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> categoriaService.excluir(1L));
        assertTrue(ex.getMessage().contains("chamados vinculados"));
    }

    @Test
    void deveLancarExcecaoAoCadastrarSetorComSiglaDuplicada() {
        when(setorRepository.existsBySiglaIgnoreCase("COTIC")).thenReturn(true);

        assertThrows(RegraNegocioException.class,
                () -> setorService.criar(new SetorRequest("TI", "COTIC")));
    }
}
