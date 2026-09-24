package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.Comparacao;
import br.com.squadcore.comparaprecos.entity.Cotacao;
import br.com.squadcore.comparaprecos.repository.CotacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Download da tabela de uma comparação já executada (RF13, CT25).
 *
 * Lê a comparação gravada pelo ComparacaoService (resultados, cálculos e configuração) e só a
 * formata: nenhuma regra é aplicada, nenhum cálculo é refeito e a ordem registrada é mantida.
 * Por isso uma comparação antiga continua igual depois de alterações em regras, parâmetros,
 * cadastros ou de novas comparações.
 */
@Service
public class ExportacaoComparacaoService {

    private final ComparacaoService comparacaoService;
    private final CotacaoRepository cotacaoRepository;
    private final TabelaComparacaoCsv tabela;

    public ExportacaoComparacaoService(ComparacaoService comparacaoService, CotacaoRepository cotacaoRepository,
                                       TabelaComparacaoCsv tabela) {
        this.comparacaoService = comparacaoService;
        this.cotacaoRepository = cotacaoRepository;
        this.tabela = tabela;
    }

    public record Arquivo(String nome, byte[] conteudo) {
    }

    @Transactional(readOnly = true)
    public Arquivo exportar(Long comparacaoId) {
        ComparacaoDetalhada detalhe = comparacaoService.buscar(comparacaoId);
        Comparacao comparacao = detalhe.comparacao();
        // A descrição da cotação não é alterável (coluna updatable = false).
        String descricao = cotacaoRepository.findById(comparacao.getCotacaoId()).map(Cotacao::getDescricao).orElse(null);
        return new Arquivo(tabela.nomeArquivo(comparacao), tabela.gerar(comparacao, descricao, detalhe.calculos()));
    }
}
