package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.calculation.OperacaoTributavel;
import br.com.squadcore.comparaprecos.calculation.SelecaoTributo;
import br.com.squadcore.comparaprecos.calculation.SeletorRegrasTributarias;
import br.com.squadcore.comparaprecos.dto.RegraTributariaRequest;
import br.com.squadcore.comparaprecos.dto.RegrasAplicaveisRequest;
import br.com.squadcore.comparaprecos.entity.AbrangenciaUf;
import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.FormaAliquota;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.Tributo;
import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.exception.CampoInvalidoException;
import br.com.squadcore.comparaprecos.exception.RegraTributariaJaCadastradaException;
import br.com.squadcore.comparaprecos.exception.RegraTributariaNaoEncontradaException;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import br.com.squadcore.comparaprecos.repository.RegraTributariaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Cadastro das regras tributárias configuráveis (REGRAS_TRIBUTARIAS §9).
 *
 * Aqui só se valida a coerência da configuração; nenhum valor tributário é definido no código.
 * Regras desativadas continuam visíveis e editáveis, para o ADMIN configurá-las e ativá-las.
 */
@Service
public class RegraTributariaService {

    private static final String CONSTRAINT_NOME_UNICO = "ux_regra_tributaria_nome";

    private final RegraTributariaRepository regraRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ProdutoRepository produtoRepository;
    private final ParametroCalculoService parametroService;
    private final SeletorRegrasTributarias seletor;

    public RegraTributariaService(RegraTributariaRepository regraRepository,
                                  FornecedorRepository fornecedorRepository,
                                  ProdutoRepository produtoRepository,
                                  ParametroCalculoService parametroService,
                                  SeletorRegrasTributarias seletor) {
        this.regraRepository = regraRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.produtoRepository = produtoRepository;
        this.parametroService = parametroService;
        this.seletor = seletor;
    }

    /** Filtros opcionais; ordenadas por tributo, prioridade (maior primeiro) e nome. */
    @Transactional(readOnly = true)
    public List<RegraTributaria> listar(Boolean ativa, Tributo tributo) {
        return regraRepository.findAllByOrderByTributoAscPrioridadeDescNomeAscIdAsc().stream()
                .filter(regra -> ativa == null || regra.isAtiva() == ativa)
                .filter(regra -> tributo == null || regra.getTributo() == tributo)
                .toList();
    }

    @Transactional(readOnly = true)
    public RegraTributaria buscar(Long id) {
        return regraRepository.findById(id).orElseThrow(RegraTributariaNaoEncontradaException::new);
    }

    @Transactional
    public RegraTributaria criar(RegraTributariaRequest request) {
        String nome = request.nome().trim();
        verificarNomeDisponivel(nome, null);
        RegraTributaria regra = new RegraTributaria(nome, request.tributo(), request.formaAliquota());
        preencher(regra, request, true);
        return salvar(regra);
    }

    @Transactional
    public RegraTributaria atualizar(Long id, RegraTributariaRequest request) {
        RegraTributaria regra = buscar(id);
        String nome = request.nome().trim();
        verificarNomeDisponivel(nome, id);
        regra.setNome(nome);
        regra.setTributo(request.tributo());
        regra.setFormaAliquota(request.formaAliquota());
        preencher(regra, request, regra.isAtiva());
        return salvar(regra);
    }

    /** Desativação lógica: a regra é mantida, pois cálculos registram id e versão utilizados. */
    @Transactional
    public void desativar(Long id) {
        RegraTributaria regra = buscar(id);
        if (regra.isAtiva()) {
            regra.setAtiva(false);
            regraRepository.saveAndFlush(regra);
        }
    }

    /** Somente a seleção das regras (sem cálculo), para o ADMIN conferir a configuração. */
    @Transactional(readOnly = true)
    public RegrasSelecionadas regrasAplicaveis(RegrasAplicaveisRequest request) {
        Uf ufDestino = request.ufDestino() != null ? request.ufDestino() : ufDestinoParametro();
        OperacaoTributavel operacao = new OperacaoTributavel(request.fornecedorId(), request.tipoFornecedor(),
                request.produtoId(), request.ufOrigem(), ufDestino, request.origemMercadoria(), request.cfop());
        return new RegrasSelecionadas(operacao, seletor.selecionar(operacao, regraRepository.findByAtivaTrue()));
    }

    public record RegrasSelecionadas(OperacaoTributavel operacao, List<SelecaoTributo> tributos) {
    }

    private Uf ufDestinoParametro() {
        return parametroService.valor(ChaveParametro.UF_DESTINO).map(Uf::valueOf).orElse(null);
    }

    private void preencher(RegraTributaria regra, RegraTributariaRequest request, boolean ativaPadrao) {
        validarAliquota(request);
        validarAbrangencia(request);

        regra.setObservacao(normalizarTexto(request.observacao()));
        regra.setAliquota(request.aliquota());
        // Fator 1 é neutro (usa a alíquota como obtida); SEM_CREDITO não tem fator.
        regra.setFator(request.formaAliquota() == FormaAliquota.SEM_CREDITO ? null
                : Objects.requireNonNullElse(request.fator(), BigDecimal.ONE));
        regra.setPrioridade(Objects.requireNonNullElse(request.prioridade(), 0));
        regra.setAtiva(Objects.requireNonNullElse(request.ativa(), ativaPadrao));
        regra.setTipoFornecedor(request.tipoFornecedor());
        regra.setFornecedor(request.fornecedorId() == null ? null
                : fornecedorRepository.findByIdAndAtivoTrue(request.fornecedorId())
                .orElseThrow(() -> new CampoInvalidoException("fornecedorId", "Fornecedor não encontrado.")));
        regra.setProduto(request.produtoId() == null ? null
                : produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                .orElseThrow(() -> new CampoInvalidoException("produtoId", "Produto não encontrado.")));
        regra.setUfOrigem(request.ufOrigem());
        regra.setUfDestino(request.ufDestino());
        regra.setAbrangenciaUf(request.abrangenciaUf());
        regra.definirOrigensMercadoria(request.origensMercadoria() == null ? Set.of() : request.origensMercadoria());
        regra.definirCfops(request.cfops() == null ? Set.of() : request.cfops());
    }

    private static void validarAliquota(RegraTributariaRequest request) {
        boolean percentualFixo = request.formaAliquota() == FormaAliquota.PERCENTUAL_FIXO;
        if (percentualFixo && request.aliquota() == null) {
            throw new CampoInvalidoException("aliquota", "A alíquota é obrigatória para PERCENTUAL_FIXO.");
        }
        if (!percentualFixo && request.aliquota() != null) {
            throw new CampoInvalidoException("aliquota",
                    "A alíquota só pode ser informada para PERCENTUAL_FIXO.");
        }
        if (request.formaAliquota() == FormaAliquota.SEM_CREDITO && request.fator() != null) {
            throw new CampoInvalidoException("fator", "O fator não se aplica a SEM_CREDITO.");
        }
    }

    /** Condições contraditórias fariam a regra nunca se aplicar. */
    private static void validarAbrangencia(RegraTributariaRequest request) {
        if (request.abrangenciaUf() == null || request.ufOrigem() == null || request.ufDestino() == null) {
            return;
        }
        boolean mesmaUf = request.ufOrigem() == request.ufDestino();
        if (request.abrangenciaUf() == AbrangenciaUf.INTERNA && !mesmaUf) {
            throw new CampoInvalidoException("abrangenciaUf",
                    "Abrangência INTERNA exige UF de origem igual à UF de destino.");
        }
        if (request.abrangenciaUf() == AbrangenciaUf.INTERESTADUAL && mesmaUf) {
            throw new CampoInvalidoException("abrangenciaUf",
                    "Abrangência INTERESTADUAL exige UF de origem diferente da UF de destino.");
        }
    }

    private void verificarNomeDisponivel(String nome, Long idAtual) {
        regraRepository.findByNomeIgnoreCase(nome)
                .filter(existente -> !Objects.equals(existente.getId(), idAtual))
                .ifPresent(existente -> {
                    throw new RegraTributariaJaCadastradaException();
                });
    }

    /** O índice único do banco é a garantia final contra cadastros simultâneos. */
    private RegraTributaria salvar(RegraTributaria regra) {
        try {
            return regraRepository.saveAndFlush(regra);
        } catch (DataIntegrityViolationException ex) {
            String mensagem = ex.getMessage() == null ? "" : ex.getMessage();
            if (mensagem.contains(CONSTRAINT_NOME_UNICO)) {
                throw new RegraTributariaJaCadastradaException();
            }
            throw ex;
        }
    }

    private static String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
