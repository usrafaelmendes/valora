package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.dto.CalculoRequest;
import br.com.squadcore.comparaprecos.dto.CotacaoRequest;
import br.com.squadcore.comparaprecos.dto.OpcaoCotacaoRequest;
import br.com.squadcore.comparaprecos.entity.Cotacao;
import br.com.squadcore.comparaprecos.entity.CotacaoOpcao;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.NfeItem;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.exception.CampoInvalidoException;
import br.com.squadcore.comparaprecos.exception.CotacaoNaoEncontradaException;
import br.com.squadcore.comparaprecos.repository.CotacaoRepository;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.NfeItemRepository;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Cotações (RF08, CT23, CT24): criação, inclusão de opções e consulta.
 *
 * Fornecedor e produto precisam estar ativos para entrar em uma cotação (nada é reativado).
 * A cotação e as opções não são alteradas nem apagadas; os resultados ficam nas comparações.
 */
@Service
public class CotacaoService {

    private final CotacaoRepository cotacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final NfeItemRepository nfeItemRepository;
    private final ComparacaoService comparacaoService;

    public CotacaoService(CotacaoRepository cotacaoRepository, ProdutoRepository produtoRepository,
                          FornecedorRepository fornecedorRepository, NfeItemRepository nfeItemRepository,
                          ComparacaoService comparacaoService) {
        this.cotacaoRepository = cotacaoRepository;
        this.produtoRepository = produtoRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.nfeItemRepository = nfeItemRepository;
        this.comparacaoService = comparacaoService;
    }

    /** Cotação com a comparação mais recente (nula quando ainda não houve comparação). */
    public record CotacaoDetalhada(Cotacao cotacao, ComparacaoDetalhada ultimaComparacao) {
    }

    /** Cria a cotação e já executa a primeira comparação (CT23). */
    @Transactional
    public CotacaoDetalhada criar(CotacaoRequest request, Long usuarioId) {
        Produto produto = produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                .orElseThrow(() -> new CampoInvalidoException("produtoId", "Produto não encontrado ou desativado."));
        Cotacao cotacao = new Cotacao(produto, request.quantidade(), texto(request.descricao()), usuarioId);
        List<OpcaoCotacaoRequest> opcoes = request.opcoes();
        for (int i = 0; i < opcoes.size(); i++) {
            cotacao.adicionarOpcao(opcao(opcoes.get(i), produto, "opcoes[" + i + "]."));
        }
        cotacaoRepository.saveAndFlush(cotacao);
        return new CotacaoDetalhada(cotacao, comparacaoService.executar(cotacao, usuarioId));
    }

    /** Inclui uma opção; ela entra na próxima comparação executada. */
    @Transactional
    public CotacaoOpcao adicionarOpcao(Long cotacaoId, OpcaoCotacaoRequest request) {
        Cotacao cotacao = cotacaoRepository.findDetalhadaById(cotacaoId).orElseThrow(CotacaoNaoEncontradaException::new);
        Produto produto = cotacao.getProduto();
        if (!produto.isAtivo()) {
            throw new CampoInvalidoException("produtoId",
                    "O produto da cotação está desativado: não é possível incluir novas opções.");
        }
        if (cotacao.getOpcoes().size() >= CotacaoRequest.MAXIMO_OPCOES) {
            throw new CampoInvalidoException("opcoes",
                    "A cotação já tem o máximo de " + CotacaoRequest.MAXIMO_OPCOES + " opções.");
        }
        CotacaoOpcao opcao = opcao(request, produto, "");
        cotacao.adicionarOpcao(opcao);
        // A cotação já está gerenciada: o flush persiste esta mesma instância em cascata e ela
        // recebe o id. (saveAndFlush faria merge, e o merge em cascata grava uma cópia da opção,
        // deixando a instância devolvida sem id.)
        cotacaoRepository.flush();
        return opcao;
    }

    @Transactional(readOnly = true)
    public CotacaoDetalhada buscar(Long id) {
        Cotacao cotacao = cotacaoRepository.findDetalhadaById(id).orElseThrow(CotacaoNaoEncontradaException::new);
        Optional<ComparacaoDetalhada> ultima = comparacaoService.ultimaDaCotacao(id);
        return new CotacaoDetalhada(cotacao, ultima.orElse(null));
    }

    /** Da mais recente para a mais antiga; filtro opcional por produto. */
    @Transactional(readOnly = true)
    public List<Cotacao> listar(Long produtoId) {
        return produtoId == null
                ? cotacaoRepository.findAllByOrderByIdDesc()
                : cotacaoRepository.findByProdutoIdOrderByIdDesc(produtoId);
    }

    private CotacaoOpcao opcao(OpcaoCotacaoRequest request, Produto produto, String prefixo) {
        Fornecedor fornecedor;
        if (request.nfeItemId() != null) {
            NfeItem item = nfeItemRepository.findDetalhadoById(request.nfeItemId())
                    .orElseThrow(() -> new CampoInvalidoException(prefixo + "nfeItemId", "Item de NF-e não encontrado."));
            fornecedor = item.getNfe().getFornecedor();
            if (request.fornecedorId() != null && !request.fornecedorId().equals(fornecedor.getId())) {
                throw new CampoInvalidoException(prefixo + "fornecedorId",
                        "O fornecedor informado não é o emitente da NF-e do item.");
            }
            if (item.getProduto() == null || !item.getProduto().getId().equals(produto.getId())) {
                throw new CampoInvalidoException(prefixo + "nfeItemId",
                        "O item da NF-e não está vinculado ao produto da cotação.");
            }
            if (!fornecedor.isAtivo()) {
                throw new CampoInvalidoException(prefixo + "nfeItemId",
                        "O fornecedor emitente da NF-e está desativado.");
            }
        } else {
            if (request.fornecedorId() == null) {
                throw new CampoInvalidoException(prefixo + "fornecedorId",
                        "Informe o fornecedor ou o item de NF-e da opção.");
            }
            fornecedor = fornecedorRepository.findByIdAndAtivoTrue(request.fornecedorId())
                    .orElseThrow(() -> new CampoInvalidoException(prefixo + "fornecedorId",
                            "Fornecedor não encontrado ou desativado."));
        }
        CotacaoOpcao opcao = new CotacaoOpcao(fornecedor, request.nfeItemId(), texto(request.condicaoPagamento()),
                texto(request.observacao()));
        if (request.valores() != null) {
            CalculoRequest.ValoresInformados v = request.valores();
            opcao.definirValores(v.valorProduto(), v.valorIpi(), v.valorFrete(), v.valorSeguro(),
                    v.valorOutrasDespesas(), v.valorDesconto());
        }
        if (request.dadosFiscais() != null) {
            CalculoRequest.DadosFiscaisInformados d = request.dadosFiscais();
            opcao.definirDadosFiscais(d.origemMercadoria(), d.cfop(), d.aliquotaIcms(), d.aliquotaIpi(),
                    d.aliquotaPis(), d.aliquotaCofins());
        }
        return opcao;
    }

    /** Texto em branco é tratado como não informado (CT22). */
    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
