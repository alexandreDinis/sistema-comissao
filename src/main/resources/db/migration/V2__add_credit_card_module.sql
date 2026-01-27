/*
 * V2__add_credit_card_module.sql
 * Módulo de Cartão de Crédito Corporativo
 * Data: 2026-01-18
 */

-- ===========================================
-- CARTÕES DE CRÉDITO
-- ===========================================
CREATE TABLE cartoes_credito (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    nome VARCHAR(100) NOT NULL,
    dia_vencimento INT NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 28),
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP
);

CREATE INDEX idx_cartao_empresa ON cartoes_credito(empresa_id, ativo);

-- ===========================================
-- ALTERAÇÕES EM DESPESAS (referência ao cartão)
-- ===========================================
ALTER TABLE despesas ADD COLUMN cartao_id BIGINT REFERENCES cartoes_credito(id);

CREATE INDEX idx_despesa_cartao ON despesas(cartao_id);

-- ===========================================
-- ALTERAÇÕES EM CONTAS A PAGAR (para faturas de cartão)
-- ===========================================
ALTER TABLE contas_pagar ADD COLUMN cartao_id BIGINT REFERENCES cartoes_credito(id);
ALTER TABLE contas_pagar ADD COLUMN mes_referencia VARCHAR(7); -- "2026-01"

CREATE INDEX idx_conta_pagar_cartao ON contas_pagar(cartao_id, mes_referencia);

-- Comentários para documentação
COMMENT ON TABLE cartoes_credito IS 'Cartões de crédito corporativos cadastrados';
COMMENT ON COLUMN despesas.cartao_id IS 'Referência ao cartão usado (se aplicável)';
COMMENT ON COLUMN contas_pagar.mes_referencia IS 'Mês de referência para faturas de cartão (YYYY-MM)';
