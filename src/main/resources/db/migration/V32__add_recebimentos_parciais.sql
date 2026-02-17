-- ============================================================
-- V32: Suporte a Recebimentos Parciais e Histórico de Pagamentos
-- ============================================================

-- 1. Nova tabela: registra cada entrada de dinheiro individualmente
CREATE TABLE recebimentos (
    id BIGSERIAL PRIMARY KEY,
    conta_receber_id BIGINT NOT NULL REFERENCES contas_receber(id),
    valor_pago NUMERIC(19,2) NOT NULL,
    data_pagamento DATE NOT NULL,
    meio_pagamento VARCHAR(30),
    observacao TEXT,
    usuario_id BIGINT REFERENCES users(id),
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    funcionario_responsavel_id BIGINT REFERENCES users(id),
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recebimentos_empresa_data ON recebimentos(empresa_id, data_pagamento);
CREATE INDEX idx_recebimentos_conta ON recebimentos(conta_receber_id);

-- 2. Novos campos em contas_receber
ALTER TABLE contas_receber ADD COLUMN valor_pago_acumulado NUMERIC(19,2) NOT NULL DEFAULT 0;
ALTER TABLE contas_receber ADD COLUMN saldo_restante NUMERIC(19,2);
ALTER TABLE contas_receber ADD COLUMN observacao TEXT;
ALTER TABLE contas_receber ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 3. Migration retroativa: preencher campos para dados existentes
-- Contas PENDENTE: saldo = valor total (nada foi pago)
UPDATE contas_receber
SET saldo_restante = valor
WHERE status = 'PENDENTE';

-- Contas PAGO: saldo = 0, acumulado = valor total
UPDATE contas_receber
SET saldo_restante = 0, valor_pago_acumulado = valor
WHERE status = 'PAGO';

-- Contas CANCELADO ou qualquer outro: saldo = valor (segurança)
UPDATE contas_receber
SET saldo_restante = valor
WHERE saldo_restante IS NULL;

-- 4. Criar recebimentos retroativos para contas já PAGO
-- Isso garante que as queries de comissão (baseadas em recebimentos) continuem funcionando
INSERT INTO recebimentos (conta_receber_id, valor_pago, data_pagamento, meio_pagamento, empresa_id, funcionario_responsavel_id, data_criacao)
SELECT
    cr.id,
    cr.valor,
    COALESCE(cr.data_recebimento, cr.data_vencimento),
    cr.meio_pagamento,
    cr.empresa_id,
    cr.funcionario_responsavel_id,
    COALESCE(cr.data_criacao, NOW())
FROM contas_receber cr
WHERE cr.status = 'PAGO';
