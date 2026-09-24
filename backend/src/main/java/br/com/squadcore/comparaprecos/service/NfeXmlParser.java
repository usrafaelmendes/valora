package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.exception.NfeInvalidaException;
import br.com.squadcore.comparaprecos.validation.CnpjUtils;
import br.com.squadcore.comparaprecos.validation.GtinUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lê o XML de uma NF-e modelo 55, layout 4.00 (RF05, RF06), com ou sem o protocolo de
 * autorização ({@code nfeProc} ou {@code NFe}), e extrai somente os campos usados pelo MVP.
 *
 * Os valores tributários são devolvidos exatamente como estão no XML: nenhuma regra de
 * crédito é aplicada aqui, e um campo ausente é devolvido como null, nunca como zero.
 *
 * O parser é o DOM do próprio JDK, sem DOCTYPE e sem acesso a entidades ou arquivos
 * externos (proteção contra XXE). A assinatura digital não é verificada.
 */
@Component
public class NfeXmlParser {

    static final String NAMESPACE_NFE = "http://www.portalfiscal.inf.br/nfe";
    private static final String VERSAO_LAYOUT = "4.00";
    private static final String MODELO_NFE = "55";

    private static final Pattern CHAVE_ACESSO = Pattern.compile("[0-9]{44}");
    private static final Pattern NCM = Pattern.compile("[0-9]{2}([0-9]{6})?");
    private static final Pattern CFOP = Pattern.compile("[0-9]{4}");
    private static final Pattern CST = Pattern.compile("[0-9]{2}");
    private static final Pattern CSOSN = Pattern.compile("[0-9]{3}");
    private static final Pattern ORIGEM_ICMS = Pattern.compile("[0-8]");
    private static final Pattern UF_DESTINATARIO = Pattern.compile("[A-Z]{2}");
    private static final Pattern DECIMAL = Pattern.compile("[0-9]+(\\.[0-9]+)?");
    private static final Pattern INTEIRO = Pattern.compile("[0-9]+");

    public NfeLida ler(byte[] conteudo) {
        Element infNFe = localizarInfNFe(parse(conteudo));

        if (!VERSAO_LAYOUT.equals(infNFe.getAttribute("versao"))) {
            throw new NfeInvalidaException("Versão de layout da NF-e não suportada. Somente a versão "
                    + VERSAO_LAYOUT + " é aceita.");
        }
        String chaveAcesso = chaveAcesso(infNFe);

        Element ide = obrigatorio(infNFe, "ide", "infNFe");
        if (!MODELO_NFE.equals(texto(ide, "mod", 2, "ide"))) {
            throw new NfeInvalidaException("Somente NF-e modelo 55 é aceita.");
        }
        int serie = inteiro(ide, "serie", 0, 999, "ide");
        int numero = inteiro(ide, "nNF", 1, 999_999_999, "ide");
        Instant dataEmissao = dataHora(ide, "dhEmi", "ide");
        String naturezaOperacao = texto(ide, "natOp", 60, "ide");

        Element emit = obrigatorio(infNFe, "emit", "infNFe");
        if (filho(emit, "CNPJ") == null) {
            throw new NfeInvalidaException("O emitente da NF-e não possui CNPJ. Somente emitentes com CNPJ são aceitos.");
        }
        String emitenteCnpj = cnpj(emit, "emit");
        if (!chaveAcesso.substring(6, 20).equals(emitenteCnpj)) {
            throw new NfeInvalidaException("A chave de acesso não corresponde ao CNPJ do emitente.");
        }
        Uf emitenteUf = ufEmitente(obrigatorio(emit, "enderEmit", "emit"));

        String destinatarioCnpj = null;
        String destinatarioUf = null;
        Element dest = filho(infNFe, "dest");
        if (dest != null) {
            destinatarioCnpj = filho(dest, "CNPJ") == null ? null : cnpj(dest, "dest");
            Element enderDest = filho(dest, "enderDest");
            if (enderDest != null) {
                destinatarioUf = texto(enderDest, "UF", 2, "dest/enderDest");
                if (!UF_DESTINATARIO.matcher(destinatarioUf).matches()) {
                    throw campoInvalido("dest/enderDest/UF");
                }
            }
        }

        List<ItemLido> itens = itens(infNFe);

        Element icmsTot = obrigatorio(obrigatorio(infNFe, "total", "infNFe"), "ICMSTot", "total");
        String caminhoTotal = "total/ICMSTot";
        return new NfeLida(chaveAcesso, numero, serie, dataEmissao, naturezaOperacao,
                emitenteCnpj, emitenteUf, destinatarioCnpj, destinatarioUf,
                valor(icmsTot, "vProd", caminhoTotal),
                valor(icmsTot, "vFrete", caminhoTotal),
                valor(icmsTot, "vSeg", caminhoTotal),
                valor(icmsTot, "vDesc", caminhoTotal),
                valor(icmsTot, "vOutro", caminhoTotal),
                valor(icmsTot, "vIPI", caminhoTotal),
                valor(icmsTot, "vNF", caminhoTotal),
                itens);
    }

    // ---- Leitura segura do documento ----

    private static Document parse(byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new NfeInvalidaException("O arquivo enviado está vazio.");
        }
        try {
            DocumentBuilder builder = fabricaSegura().newDocumentBuilder();
            builder.setErrorHandler(SEM_SAIDA_NO_CONSOLE);
            return builder.parse(new ByteArrayInputStream(conteudo));
        } catch (SAXException | IOException ex) {
            // A mensagem do parser pode conter trechos do arquivo: não é repassada ao usuário.
            throw new NfeInvalidaException("O arquivo enviado não é um XML válido.");
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException("Parser XML sem suporte à configuração segura", ex);
        }
    }

    /** Recomendações OWASP contra XXE: DOCTYPE proibido e nenhum acesso externo. */
    private static DocumentBuilderFactory fabricaSegura() throws ParserConfigurationException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        return fabrica;
    }

    /** Evita que o parser escreva "[Fatal Error]" no console; os erros viram exceção. */
    private static final ErrorHandler SEM_SAIDA_NO_CONSOLE = new ErrorHandler() {
        @Override
        public void warning(SAXParseException ex) {
        }

        @Override
        public void error(SAXParseException ex) throws SAXException {
            throw ex;
        }

        @Override
        public void fatalError(SAXParseException ex) throws SAXException {
            throw ex;
        }
    };

    private static Element localizarInfNFe(Document documento) {
        Element raiz = documento.getDocumentElement();
        Element nfe = null;
        if (NAMESPACE_NFE.equals(raiz.getNamespaceURI())) {
            nfe = switch (raiz.getLocalName()) {
                case "nfeProc" -> filho(raiz, "NFe");
                case "NFe" -> raiz;
                default -> null;
            };
        }
        Element infNFe = nfe == null ? null : filho(nfe, "infNFe");
        if (infNFe == null) {
            throw new NfeInvalidaException("O XML enviado não é uma NF-e.");
        }
        return infNFe;
    }

    // ---- Grupos da NF-e ----

    /** O atributo Id é "NFe" seguido da chave; o último dígito é o verificador (módulo 11). */
    private static String chaveAcesso(Element infNFe) {
        String id = infNFe.getAttribute("Id");
        String chave = id.startsWith("NFe") ? id.substring(3) : "";
        if (!CHAVE_ACESSO.matcher(chave).matches() || !digitoVerificadorValido(chave)) {
            throw new NfeInvalidaException("A chave de acesso da NF-e (atributo Id de infNFe) está ausente ou é inválida.");
        }
        return chave;
    }

    static boolean digitoVerificadorValido(String chave) {
        int soma = 0;
        int peso = 2;
        for (int i = 42; i >= 0; i--) {
            soma += (chave.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        int digito = resto < 2 ? 0 : 11 - resto;
        return chave.charAt(43) - '0' == digito;
    }

    private static Uf ufEmitente(Element enderEmit) {
        String uf = texto(enderEmit, "UF", 2, "emit/enderEmit");
        try {
            return Uf.valueOf(uf);
        } catch (IllegalArgumentException ex) {
            throw campoInvalido("emit/enderEmit/UF");
        }
    }

    private static List<ItemLido> itens(Element infNFe) {
        List<ItemLido> itens = new ArrayList<>();
        Set<Integer> numeros = new HashSet<>();
        for (Element det : filhos(infNFe, "det")) {
            String caminho = "det[nItem=" + det.getAttribute("nItem") + "]";
            int numeroItem = inteiro(det.getAttribute("nItem"), 1, 990, caminho + "/@nItem");
            if (!numeros.add(numeroItem)) {
                throw new NfeInvalidaException("NF-e inválida: número de item repetido (" + numeroItem + ").");
            }
            itens.add(item(det, numeroItem, caminho));
        }
        if (itens.isEmpty()) {
            throw new NfeInvalidaException("NF-e inválida: a nota não possui itens (det).");
        }
        return itens;
    }

    private static ItemLido item(Element det, int numeroItem, String caminho) {
        Element prod = obrigatorio(det, "prod", caminho);
        String caminhoProd = caminho + "/prod";

        String ncm = texto(prod, "NCM", 8, caminhoProd);
        if (!NCM.matcher(ncm).matches()) {
            throw campoInvalido(caminhoProd + "/NCM");
        }
        String cfop = texto(prod, "CFOP", 4, caminhoProd);
        if (!CFOP.matcher(cfop).matches()) {
            throw campoInvalido(caminhoProd + "/CFOP");
        }

        Element imposto = obrigatorio(det, "imposto", caminho);
        String caminhoImposto = caminho + "/imposto";
        return new ItemLido(
                numeroItem,
                texto(prod, "cProd", 60, caminhoProd),
                gtin(prod),
                texto(prod, "xProd", 120, caminhoProd),
                ncm,
                cfop,
                texto(prod, "uCom", 6, caminhoProd),
                decimal(prod, "qCom", 11, 4, caminhoProd),
                decimal(prod, "vUnCom", 11, 10, caminhoProd),
                valor(prod, "vProd", caminhoProd),
                valorOpcional(prod, "vFrete", caminhoProd),
                valorOpcional(prod, "vSeg", caminhoProd),
                valorOpcional(prod, "vDesc", caminhoProd),
                valorOpcional(prod, "vOutro", caminhoProd),
                icms(filho(imposto, "ICMS"), caminhoImposto + "/ICMS"),
                ipi(filho(imposto, "IPI"), caminhoImposto + "/IPI"),
                tributo(filho(imposto, "PIS"), "pPIS", "vPIS", caminhoImposto + "/PIS"),
                tributo(filho(imposto, "COFINS"), "pCOFINS", "vCOFINS", caminhoImposto + "/COFINS"));
    }

    /** "SEM GTIN", código ausente ou inválido não identificam produto: o GTIN fica nulo. */
    private static String gtin(Element prod) {
        String cEAN = textoOpcional(prod, "cEAN");
        return GtinUtils.isValido(cEAN) ? GtinUtils.normalizar(cEAN) : null;
    }

    /** O grupo muda conforme a situação tributária (ICMS00, ICMS20, ICMSSN102...): lê o que existir. */
    private static Icms icms(Element icms, String caminho) {
        Element grupo = icms == null ? null : primeiroFilho(icms);
        if (grupo == null) {
            return Icms.AUSENTE;
        }
        String caminhoGrupo = caminho + "/" + grupo.getLocalName();
        return new Icms(
                codigo(grupo, "orig", ORIGEM_ICMS, caminhoGrupo),
                codigo(grupo, "CST", CST, caminhoGrupo),
                codigo(grupo, "CSOSN", CSOSN, caminhoGrupo),
                valorOpcional(grupo, "vBC", caminhoGrupo),
                aliquotaOpcional(grupo, "pICMS", caminhoGrupo),
                valorOpcional(grupo, "vICMS", caminhoGrupo));
    }

    /** IPI tributado (IPITrib) ou não tributado (IPINT); o grupo IPI inteiro é opcional. */
    private static Tributo ipi(Element ipi, String caminho) {
        if (ipi == null) {
            return Tributo.AUSENTE;
        }
        Element grupo = filho(ipi, "IPITrib");
        if (grupo == null) {
            grupo = filho(ipi, "IPINT");
        }
        if (grupo == null) {
            return Tributo.AUSENTE;
        }
        String caminhoGrupo = caminho + "/" + grupo.getLocalName();
        return new Tributo(
                codigo(grupo, "CST", CST, caminhoGrupo),
                valorOpcional(grupo, "vBC", caminhoGrupo),
                aliquotaOpcional(grupo, "pIPI", caminhoGrupo),
                valorOpcional(grupo, "vIPI", caminhoGrupo));
    }

    /** PIS e COFINS: o grupo varia (PISAliq, PISOutr, PISNT...): lê o que existir. */
    private static Tributo tributo(Element tributo, String campoAliquota, String campoValor, String caminho) {
        Element grupo = tributo == null ? null : primeiroFilho(tributo);
        if (grupo == null) {
            return Tributo.AUSENTE;
        }
        String caminhoGrupo = caminho + "/" + grupo.getLocalName();
        return new Tributo(
                codigo(grupo, "CST", CST, caminhoGrupo),
                valorOpcional(grupo, "vBC", caminhoGrupo),
                aliquotaOpcional(grupo, campoAliquota, caminhoGrupo),
                valorOpcional(grupo, campoValor, caminhoGrupo));
    }

    // ---- Leitura de campos ----

    private static Element filho(Element pai, String nome) {
        for (Node no = pai.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element elemento && nome.equals(elemento.getLocalName())
                    && NAMESPACE_NFE.equals(elemento.getNamespaceURI())) {
                return elemento;
            }
        }
        return null;
    }

    private static List<Element> filhos(Element pai, String nome) {
        List<Element> encontrados = new ArrayList<>();
        for (Node no = pai.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element elemento && nome.equals(elemento.getLocalName())
                    && NAMESPACE_NFE.equals(elemento.getNamespaceURI())) {
                encontrados.add(elemento);
            }
        }
        return encontrados;
    }

    private static Element primeiroFilho(Element pai) {
        for (Node no = pai.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element elemento && NAMESPACE_NFE.equals(elemento.getNamespaceURI())) {
                return elemento;
            }
        }
        return null;
    }

    private static Element obrigatorio(Element pai, String nome, String caminho) {
        Element elemento = filho(pai, nome);
        if (elemento == null) {
            throw campoAusente(caminho + "/" + nome);
        }
        return elemento;
    }

    private static String textoOpcional(Element pai, String nome) {
        Element elemento = filho(pai, nome);
        if (elemento == null) {
            return null;
        }
        String valor = elemento.getTextContent().trim();
        return valor.isEmpty() ? null : valor;
    }

    private static String texto(Element pai, String nome, int tamanhoMaximo, String caminho) {
        String valor = textoOpcional(pai, nome);
        if (valor == null) {
            throw campoAusente(caminho + "/" + nome);
        }
        if (valor.length() > tamanhoMaximo) {
            throw campoInvalido(caminho + "/" + nome);
        }
        return valor;
    }

    private static String codigo(Element pai, String nome, Pattern formato, String caminho) {
        String valor = textoOpcional(pai, nome);
        if (valor != null && !formato.matcher(valor).matches()) {
            throw campoInvalido(caminho + "/" + nome);
        }
        return valor;
    }

    private static String cnpj(Element pai, String caminho) {
        String cnpj = CnpjUtils.normalizar(texto(pai, "CNPJ", 14, caminho));
        if (!CnpjUtils.isValido(cnpj)) {
            throw campoInvalido(caminho + "/CNPJ");
        }
        return cnpj;
    }

    private static int inteiro(Element pai, String nome, int minimo, int maximo, String caminho) {
        return inteiro(texto(pai, nome, 9, caminho), minimo, maximo, caminho + "/" + nome);
    }

    private static int inteiro(String valor, int minimo, int maximo, String caminhoCampo) {
        if (valor == null || valor.isBlank()) {
            throw campoAusente(caminhoCampo);
        }
        if (!INTEIRO.matcher(valor).matches() || valor.length() > 9) {
            throw campoInvalido(caminhoCampo);
        }
        int numero = Integer.parseInt(valor);
        if (numero < minimo || numero > maximo) {
            throw campoInvalido(caminhoCampo);
        }
        return numero;
    }

    private static Instant dataHora(Element pai, String nome, String caminho) {
        String valor = texto(pai, nome, 25, caminho);
        try {
            return OffsetDateTime.parse(valor).toInstant();
        } catch (DateTimeParseException ex) {
            throw campoInvalido(caminho + "/" + nome);
        }
    }

    /** Valor monetário obrigatório: até 13 inteiros e 2 decimais (TDec_1302 do layout). */
    private static BigDecimal valor(Element pai, String nome, String caminho) {
        return decimal(pai, nome, 13, 2, caminho);
    }

    private static BigDecimal valorOpcional(Element pai, String nome, String caminho) {
        return filho(pai, nome) == null ? null : decimal(pai, nome, 13, 2, caminho);
    }

    /** Alíquota percentual: até 3 inteiros e 4 decimais (TDec_0302a04 do layout). */
    private static BigDecimal aliquotaOpcional(Element pai, String nome, String caminho) {
        return filho(pai, nome) == null ? null : decimal(pai, nome, 3, 4, caminho);
    }

    private static BigDecimal decimal(Element pai, String nome, int inteiros, int decimais, String caminho) {
        String valor = textoOpcional(pai, nome);
        if (valor == null) {
            throw campoAusente(caminho + "/" + nome);
        }
        if (!DECIMAL.matcher(valor).matches()) {
            throw campoInvalido(caminho + "/" + nome);
        }
        BigDecimal numero = new BigDecimal(valor);
        if (numero.scale() > decimais || numero.precision() - numero.scale() > inteiros) {
            throw campoInvalido(caminho + "/" + nome);
        }
        return numero;
    }

    private static NfeInvalidaException campoAusente(String caminho) {
        return new NfeInvalidaException("NF-e inválida: campo obrigatório ausente (" + caminho + ").");
    }

    private static NfeInvalidaException campoInvalido(String caminho) {
        return new NfeInvalidaException("NF-e inválida: campo com valor inválido (" + caminho + ").");
    }

    // ---- Dados extraídos ----

    public record NfeLida(String chaveAcesso, int numero, int serie, Instant dataEmissao,
                          String naturezaOperacao, String emitenteCnpj, Uf emitenteUf,
                          String destinatarioCnpj, String destinatarioUf,
                          BigDecimal valorProdutos, BigDecimal valorFrete, BigDecimal valorSeguro,
                          BigDecimal valorDesconto, BigDecimal valorOutrasDespesas, BigDecimal valorIpi,
                          BigDecimal valorTotal, List<ItemLido> itens) {
    }

    /**
     * gtin é nulo quando o cEAN não existe, é "SEM GTIN" ou não é um GTIN válido.
     * valorFrete, valorSeguro, valorDesconto e valorOutrasDespesas são nulos quando a tag não existe no item.
     */
    public record ItemLido(int numeroItem, String codigoProdutoFornecedor, String gtin, String descricao,
                           String ncm, String cfop, String unidade, BigDecimal quantidade,
                           BigDecimal valorUnitario, BigDecimal valorProduto,
                           BigDecimal valorFrete, BigDecimal valorSeguro, BigDecimal valorDesconto,
                           BigDecimal valorOutrasDespesas,
                           Icms icms, Tributo ipi, Tributo pis, Tributo cofins) {
    }

    /** Campos do grupo ICMS como estão no XML; null quando ausentes. */
    public record Icms(String origem, String cst, String csosn, BigDecimal baseCalculo,
                       BigDecimal aliquota, BigDecimal valor) {
        static final Icms AUSENTE = new Icms(null, null, null, null, null, null);
    }

    /** Campos de IPI, PIS ou COFINS como estão no XML; null quando ausentes. */
    public record Tributo(String cst, BigDecimal baseCalculo, BigDecimal aliquota, BigDecimal valor) {
        static final Tributo AUSENTE = new Tributo(null, null, null, null);
    }
}
