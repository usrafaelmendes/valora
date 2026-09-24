package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.Nfe;
import br.com.squadcore.comparaprecos.entity.NfeItem;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.exception.FornecedorDaNfeNaoCadastradoException;
import br.com.squadcore.comparaprecos.exception.NfeJaImportadaException;
import br.com.squadcore.comparaprecos.exception.NfeNaoEncontradaException;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.NfeRepository;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import br.com.squadcore.comparaprecos.service.NfeXmlParser.ItemLido;
import br.com.squadcore.comparaprecos.service.NfeXmlParser.NfeLida;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Importação manual de NF-e (RF05 a RF07, RF14): lê o XML, identifica o fornecedor pelo
 * CNPJ do emitente, vincula os itens aos produtos pelo GTIN e grava tudo em uma transação.
 *
 * Nenhuma regra tributária é aplicada aqui: os dados ficam como estão no XML para as
 * etapas de regras tributárias e cálculo.
 */
@Service
public class NfeService {

    private static final String CONSTRAINT_CHAVE_UNICA = "ux_nfe_chave_acesso";

    private final NfeXmlParser parser;
    private final NfeRepository nfeRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ProdutoRepository produtoRepository;

    public NfeService(NfeXmlParser parser, NfeRepository nfeRepository,
                      FornecedorRepository fornecedorRepository, ProdutoRepository produtoRepository) {
        this.parser = parser;
        this.nfeRepository = nfeRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.produtoRepository = produtoRepository;
    }

    /** Em qualquer erro, nada é gravado: a nota e os itens são revertidos juntos. */
    @Transactional
    public Nfe importar(byte[] xml) {
        NfeLida dados = parser.ler(xml);

        // Reimportação da mesma chave: rejeitada (a NF-e importada é histórica e imutável).
        if (nfeRepository.existsByChaveAcesso(dados.chaveAcesso())) {
            throw new NfeJaImportadaException();
        }
        Fornecedor fornecedor = fornecedorEmitente(dados.emitenteCnpj());

        Nfe nfe = new Nfe(dados.chaveAcesso(), dados.numero(), dados.serie(), dados.dataEmissao(),
                dados.naturezaOperacao(), fornecedor, dados.emitenteCnpj(), dados.emitenteUf(),
                dados.destinatarioCnpj(), dados.destinatarioUf());
        nfe.definirTotais(dados.valorProdutos(), dados.valorFrete(), dados.valorSeguro(),
                dados.valorDesconto(), dados.valorOutrasDespesas(), dados.valorIpi(), dados.valorTotal());
        for (ItemLido item : dados.itens()) {
            nfe.adicionarItem(item(item));
        }
        return salvar(nfe);
    }

    @Transactional(readOnly = true)
    public List<Nfe> listar() {
        return nfeRepository.findAllByOrderByDataEmissaoDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Nfe buscar(Long id) {
        return nfeRepository.findDetalhadaById(id).orElseThrow(NfeNaoEncontradaException::new);
    }

    /** O fornecedor nunca é criado a partir do XML: precisa estar cadastrado e ativo. */
    private Fornecedor fornecedorEmitente(String cnpj) {
        Fornecedor fornecedor = fornecedorRepository.findByCnpj(cnpj)
                .orElseThrow(() -> FornecedorDaNfeNaoCadastradoException.naoCadastrado(cnpj));
        if (!fornecedor.isAtivo()) {
            throw FornecedorDaNfeNaoCadastradoException.desativado(cnpj);
        }
        return fornecedor;
    }

    private NfeItem item(ItemLido lido) {
        NfeItem item = new NfeItem(lido.numeroItem(), produtoPorGtin(lido.gtin()),
                lido.codigoProdutoFornecedor(), lido.gtin(), lido.descricao(), lido.ncm(), lido.cfop(),
                lido.unidade(), lido.quantidade(), lido.valorUnitario(), lido.valorProduto());
        item.definirComponentes(lido.valorFrete(), lido.valorSeguro(), lido.valorDesconto(),
                lido.valorOutrasDespesas());
        item.definirIcms(lido.icms().origem(), lido.icms().cst(), lido.icms().csosn(),
                lido.icms().baseCalculo(), lido.icms().aliquota(), lido.icms().valor());
        item.definirIpi(lido.ipi().cst(), lido.ipi().baseCalculo(), lido.ipi().aliquota(), lido.ipi().valor());
        item.definirPis(lido.pis().cst(), lido.pis().baseCalculo(), lido.pis().aliquota(), lido.pis().valor());
        item.definirCofins(lido.cofins().cst(), lido.cofins().baseCalculo(), lido.cofins().aliquota(),
                lido.cofins().valor());
        return item;
    }

    /**
     * Vínculo somente por GTIN idêntico a um produto ativo. Sem GTIN, ou sem produto
     * correspondente, o item fica sem vínculo (nunca por descrição).
     */
    private Produto produtoPorGtin(String gtin) {
        if (gtin == null) {
            return null;
        }
        return produtoRepository.findByGtin(gtin).filter(Produto::isAtivo).orElse(null);
    }

    /** O índice único do banco é a garantia final contra importações simultâneas da mesma nota. */
    private Nfe salvar(Nfe nfe) {
        try {
            return nfeRepository.saveAndFlush(nfe);
        } catch (DataIntegrityViolationException ex) {
            String mensagem = ex.getMessage() == null ? "" : ex.getMessage();
            if (mensagem.contains(CONSTRAINT_CHAVE_UNICA)) {
                throw new NfeJaImportadaException();
            }
            throw ex;
        }
    }
}
