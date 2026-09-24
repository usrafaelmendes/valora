-- Carga inicial dos parâmetros gerais do cálculo.
--
-- Nenhuma regra tributária é pré-configurada: uma instalação nova começa sem regras, e o ADMIN
-- cadastra as regras aplicáveis pela API (/regras-tributarias), sem mudança de código.
--
-- Parâmetros com valor nulo = ainda não definidos: o cálculo informa a pendência em vez de
-- presumir um valor. O ADMIN define os valores pela API (/parametros-calculo).
INSERT INTO parametro_calculo (chave, valor)
VALUES
    -- UF de destino das operações: definida pelo ADMIN.
    ('UF_DESTINO', NULL),
    -- Origem = estado de emissão da NF-e (decisão técnica, ARQUITETURA §8 e §18).
    ('FONTE_UF_ORIGEM', 'EMITENTE_NFE'),
    -- Arredondamento dos créditos: por crédito ou somente no total, definido pelo ADMIN.
    ('ARREDONDAMENTO_CREDITOS', NULL),
    -- Operações de NF-e (CFOPs) que participam do cálculo: definidas pelo ADMIN.
    ('CFOPS_PARTICIPANTES', NULL);
