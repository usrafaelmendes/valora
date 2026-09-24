package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.dto.ProdutoRequest;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.exception.ProdutoJaCadastradoException;
import br.com.squadcore.comparaprecos.exception.ProdutoNaoEncontradoException;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import br.com.squadcore.comparaprecos.validation.GtinUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Cadastro de produtos (RF04). Somente dados de identificação: nenhuma regra tributária é aplicada aqui.
 *
 * Produtos desativados não são apagados, mas deixam de aparecer nas consultas e
 * listagens e não podem ser alterados.
 */
@Service
public class ProdutoService {

    private static final String CONSTRAINT_NOME_UNICO = "ux_produto_nome";
    private static final String CONSTRAINT_GTIN_UNICO = "ux_produto_gtin";

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Produto criar(ProdutoRequest request) {
        String nome = request.nome().trim();
        String gtin = GtinUtils.normalizar(request.gtin());
        verificarDisponibilidade(nome, gtin, null);
        return salvar(new Produto(nome, normalizarTexto(request.descricao()), gtin));
    }

    @Transactional(readOnly = true)
    public List<Produto> listarAtivos() {
        return produtoRepository.findByAtivoTrueOrderByNomeAscIdAsc();
    }

    @Transactional(readOnly = true)
    public Produto buscarAtivo(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id).orElseThrow(ProdutoNaoEncontradoException::new);
    }

    @Transactional
    public Produto atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarAtivo(id);
        String nome = request.nome().trim();
        String gtin = GtinUtils.normalizar(request.gtin());
        verificarDisponibilidade(nome, gtin, id);

        produto.setNome(nome);
        produto.setDescricao(normalizarTexto(request.descricao()));
        produto.setGtin(gtin);
        return salvar(produto);
    }

    /** Desativação lógica: o registro é mantido para o histórico de NF-e e cotações (RF14). */
    @Transactional
    public void desativar(Long id) {
        Produto produto = buscarAtivo(id);
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    private void verificarDisponibilidade(String nome, String gtin, Long idAtual) {
        produtoRepository.findByNomeIgnoreCase(nome)
                .filter(existente -> !Objects.equals(existente.getId(), idAtual))
                .ifPresent(existente -> {
                    throw ProdutoJaCadastradoException.porNome(!existente.isAtivo());
                });
        if (gtin != null) {
            produtoRepository.findByGtin(gtin)
                    .filter(existente -> !Objects.equals(existente.getId(), idAtual))
                    .ifPresent(existente -> {
                        throw ProdutoJaCadastradoException.porGtin(!existente.isAtivo());
                    });
        }
    }

    /** Os índices únicos do banco são a garantia final contra cadastros simultâneos. */
    private Produto salvar(Produto produto) {
        try {
            return produtoRepository.saveAndFlush(produto);
        } catch (DataIntegrityViolationException ex) {
            String mensagem = ex.getMessage() == null ? "" : ex.getMessage();
            if (mensagem.contains(CONSTRAINT_NOME_UNICO)) {
                throw ProdutoJaCadastradoException.porNome(false);
            }
            if (mensagem.contains(CONSTRAINT_GTIN_UNICO)) {
                throw ProdutoJaCadastradoException.porGtin(false);
            }
            throw ex;
        }
    }

    /** Texto opcional em branco é tratado como não informado. */
    private static String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
