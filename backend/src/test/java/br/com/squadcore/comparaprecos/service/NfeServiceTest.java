package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.Nfe;
import br.com.squadcore.comparaprecos.entity.NfeItem;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.exception.FornecedorDaNfeNaoCadastradoException;
import br.com.squadcore.comparaprecos.exception.NfeInvalidaException;
import br.com.squadcore.comparaprecos.exception.NfeJaImportadaException;
import br.com.squadcore.comparaprecos.exception.NfeNaoEncontradaException;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.NfeRepository;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.CHAVE;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.CNPJ_EMITENTE;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.GTIN_ITEM_1;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.GTIN_ITEM_3;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Importação de NF-e com o parser real e repositórios simulados.
 * Fornecedor, produtos e a NF-e são dados de teste (fixture sintética).
 */
class NfeServiceTest {

    private final NfeRepository nfeRepository = mock(NfeRepository.class);
    private final FornecedorRepository fornecedorRepository = mock(FornecedorRepository.class);
    private final ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
    private final NfeService service = new NfeService(new NfeXmlParser(), nfeRepository,
            fornecedorRepository, produtoRepository);

    private Fornecedor fornecedor;

    @BeforeEach
    void setUp() {
        fornecedor = new Fornecedor("Fornecedor Teste Ltda", CNPJ_EMITENTE, Uf.PR, TipoFornecedor.FABRICANTE, null);
        ReflectionTestUtils.setField(fornecedor, "id", 10L);
        when(fornecedorRepository.findByCnpj(CNPJ_EMITENTE)).thenReturn(Optional.of(fornecedor));
        when(produtoRepository.findByGtin(anyString())).thenReturn(Optional.empty());
        when(nfeRepository.saveAndFlush(any(Nfe.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // Fornecedor

    @Test
    void associaNotaAoFornecedorPeloCnpjDoEmitente() {
        Nfe nfe = service.importar(bytes());

        assertThat(nfe.getFornecedor()).isSameAs(fornecedor);
        assertThat(nfe.getEmitenteCnpj()).isEqualTo(CNPJ_EMITENTE);
        verify(nfeRepository).saveAndFlush(nfe);
    }

    @Test
    void fornecedorInexistenteNaoImportaENaoCriaCadastro() {
        when(fornecedorRepository.findByCnpj(CNPJ_EMITENTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importar(bytes()))
                .isInstanceOf(FornecedorDaNfeNaoCadastradoException.class)
                .hasMessageContaining("não está cadastrado como fornecedor");
        verify(fornecedorRepository, never()).save(any());
        verify(nfeRepository, never()).saveAndFlush(any());
    }

    @Test
    void fornecedorDesativadoNaoImporta() {
        fornecedor.setAtivo(false);

        assertThatThrownBy(() -> service.importar(bytes()))
                .isInstanceOf(FornecedorDaNfeNaoCadastradoException.class)
                .hasMessageContaining("está desativado");
        verify(nfeRepository, never()).saveAndFlush(any());
    }

    // Produto

    @Test
    void vinculaItemAoProdutoEncontradoPorGtin() {
        Produto produto = produto(5L, "Produto Teste X", GTIN_ITEM_1);
        when(produtoRepository.findByGtin(GTIN_ITEM_1)).thenReturn(Optional.of(produto));

        Nfe nfe = service.importar(bytes());

        assertThat(nfe.getItens().get(0).getProduto()).isSameAs(produto);
    }

    @Test
    void itemComGtinNaoCadastradoFicaSemVinculo() {
        Nfe nfe = service.importar(bytes());

        NfeItem item = nfe.getItens().get(2);
        assertThat(item.getGtin()).isEqualTo(GTIN_ITEM_3);
        assertThat(item.getProduto()).isNull();
        verify(produtoRepository).findByGtin(GTIN_ITEM_3);
    }

    @Test
    void itemSemGtinFicaSemVinculoESemConsultaPorDescricao() {
        Nfe nfe = service.importar(bytes());

        NfeItem item = nfe.getItens().get(1);
        assertThat(item.getGtin()).isNull();
        assertThat(item.getProduto()).isNull();
        assertThat(item.getDescricao()).isEqualTo("ITEM DE TESTE SEM CODIGO DE BARRAS");
        verify(produtoRepository, never()).findByNomeIgnoreCase(anyString());
    }

    @Test
    void produtoDesativadoNaoEhVinculado() {
        Produto desativado = produto(6L, "Produto Teste B", GTIN_ITEM_3);
        desativado.setAtivo(false);
        when(produtoRepository.findByGtin(GTIN_ITEM_3)).thenReturn(Optional.of(desativado));

        assertThat(service.importar(bytes()).getItens().get(2).getProduto()).isNull();
    }

    // Persistência

    @Test
    void persisteNotaEItensComDadosOriginais() {
        Nfe nfe = service.importar(bytes());

        assertThat(nfe.getChaveAcesso()).isEqualTo(CHAVE);
        assertThat(nfe.getNumero()).isEqualTo(1234);
        assertThat(nfe.getEmitenteUf()).isEqualTo(Uf.PR);
        assertThat(nfe.getDestinatarioUf()).isEqualTo("GO");
        assertThat(nfe.getValorTotal()).isEqualByComparingTo("411.10");
        assertThat(nfe.getValorIpi()).isEqualByComparingTo("20.10");
        assertThat(nfe.getItens()).hasSize(3).allSatisfy(item -> assertThat(item.getNfe()).isSameAs(nfe));

        NfeItem item = nfe.getItens().getFirst();
        assertThat(item.getNumeroItem()).isEqualTo(1);
        assertThat(item.getIcmsOrigem()).isEqualTo("1");
        assertThat(item.getIcmsAliquota()).isEqualByComparingTo("7.00");
        assertThat(item.getIpiAliquota()).isEqualByComparingTo("10");
        assertThat(item.getPisValor()).isEqualByComparingTo("2.71");
        assertThat(item.getCofinsValor()).isEqualByComparingTo("10.05");
        // Ausência no XML continua nula (não vira zero).
        assertThat(nfe.getItens().get(1).getIcmsValor()).isNull();
        assertThat(nfe.getItens().get(2).getIpiCst()).isNull();
    }

    @Test
    void xmlInvalidoNaoConsultaNemGravaNada() {
        assertThatThrownBy(() -> service.importar(bytes("não é xml")))
                .isInstanceOf(NfeInvalidaException.class);
        verify(fornecedorRepository, never()).findByCnpj(anyString());
        verify(nfeRepository, never()).saveAndFlush(any());
    }

    // Reimportação da mesma chave de acesso: rejeitada

    @Test
    void reimportacaoDaMesmaChaveEhRejeitada() {
        when(nfeRepository.existsByChaveAcesso(CHAVE)).thenReturn(true);

        assertThatThrownBy(() -> service.importar(bytes()))
                .isInstanceOf(NfeJaImportadaException.class);
        verify(nfeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importacaoSimultaneaBarradaPeloIndiceUnico() {
        when(nfeRepository.saveAndFlush(any(Nfe.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"ux_nfe_chave_acesso\""));

        assertThatThrownBy(() -> service.importar(bytes()))
                .isInstanceOf(NfeJaImportadaException.class);
    }

    @Test
    void outraViolacaoDeIntegridadeNaoEhConvertida() {
        DataIntegrityViolationException erro = new DataIntegrityViolationException("outra constraint");
        when(nfeRepository.saveAndFlush(any(Nfe.class))).thenThrow(erro);

        assertThatThrownBy(() -> service.importar(bytes())).isSameAs(erro);
    }

    // Consulta

    @Test
    void buscaInexistenteLancaNaoEncontrada() {
        when(nfeRepository.findDetalhadaById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(NfeNaoEncontradaException.class);
    }

    private static Produto produto(Long id, String nome, String gtin) {
        Produto produto = new Produto(nome, null, gtin);
        ReflectionTestUtils.setField(produto, "id", id);
        return produto;
    }
}
