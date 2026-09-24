package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.dto.FornecedorRequest;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.exception.CnpjJaCadastradoException;
import br.com.squadcore.comparaprecos.exception.FornecedorNaoEncontradoException;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.validation.CnpjUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cadastro de fornecedores (RF03). Somente dados cadastrais: nenhuma regra tributária é aplicada aqui.
 *
 * Fornecedores desativados não são apagados, mas deixam de aparecer nas consultas e
 * listagens e não podem ser alterados.
 */
@Service
public class FornecedorService {

    private static final String CONSTRAINT_CNPJ_UNICO = "ux_fornecedor_cnpj";

    private final FornecedorRepository fornecedorRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional
    public Fornecedor criar(FornecedorRequest request) {
        String cnpj = CnpjUtils.normalizar(request.cnpj());
        verificarCnpjDisponivel(cnpj, null);
        Fornecedor fornecedor = new Fornecedor(request.razaoSocial().trim(), cnpj, request.uf(), request.tipo(),
                normalizarPrazo(request.prazoPagamentoBase()));
        return salvar(fornecedor);
    }

    /** Retorna apenas fornecedores ativos, ordenados pela razão social. */
    @Transactional(readOnly = true)
    public List<Fornecedor> listarAtivos() {
        return fornecedorRepository.findByAtivoTrueOrderByRazaoSocialAscIdAsc();
    }

    /** Reaproveitado por atualizar e desativar: fornecedor inexistente ou desativado resulta em 404. */
    @Transactional(readOnly = true)
    public Fornecedor buscarAtivo(Long id) {
        return fornecedorRepository.findByIdAndAtivoTrue(id).orElseThrow(FornecedorNaoEncontradoException::new);
    }

    @Transactional
    public Fornecedor atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = buscarAtivo(id);
        String cnpj = CnpjUtils.normalizar(request.cnpj());
        verificarCnpjDisponivel(cnpj, id);

        fornecedor.setRazaoSocial(request.razaoSocial().trim());
        fornecedor.setCnpj(cnpj);
        fornecedor.setUf(request.uf());
        fornecedor.setTipo(request.tipo());
        fornecedor.setPrazoPagamentoBase(normalizarPrazo(request.prazoPagamentoBase()));
        return salvar(fornecedor);
    }

    /** Desativação lógica: o registro é mantido para o histórico de NF-e e cotações (RF14). */
    @Transactional
    public void desativar(Long id) {
        Fornecedor fornecedor = buscarAtivo(id);
        fornecedor.setAtivo(false);
        fornecedorRepository.save(fornecedor);
    }

    private void verificarCnpjDisponivel(String cnpj, Long idAtual) {
        fornecedorRepository.findByCnpj(cnpj)
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new CnpjJaCadastradoException(!existente.isAtivo());
                });
    }

    /** O índice único do banco é a garantia final contra cadastros simultâneos do mesmo CNPJ. */
    private Fornecedor salvar(Fornecedor fornecedor) {
        try {
            return fornecedorRepository.saveAndFlush(fornecedor);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains(CONSTRAINT_CNPJ_UNICO)) {
                throw new CnpjJaCadastradoException(false);
            }
            throw ex;
        }
    }

    /** Prazo em branco é tratado como não informado (CT22). */
    private static String normalizarPrazo(String prazo) {
        return prazo == null || prazo.isBlank() ? null : prazo.trim();
    }
}
