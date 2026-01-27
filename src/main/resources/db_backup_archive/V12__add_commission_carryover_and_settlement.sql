-- V12: Adicionar campos para saldo acumulativo (carryover) e quitação de comissão
-- Autor: Sistema
-- Data: 2026-01-18

-- Adicionar saldo anterior (carryover do mês anterior)
ALTER TABLE comissoes_calculadas
ADD COLUMN IF NOT EXISTS saldo_anterior DECIMAL(19,2) DEFAULT 0;

-- Adicionar campos para quitação
ALTER TABLE comissoes_calculadas
ADD COLUMN IF NOT EXISTS quitado BOOLEAN DEFAULT FALSE;

ALTER TABLE comissoes_calculadas
ADD COLUMN IF NOT EXISTS data_quitacao TIMESTAMP;

ALTER TABLE comissoes_calculadas
ADD COLUMN IF NOT EXISTS valor_quitado DECIMAL(19,2);

-- Comentários para documentação
COMMENT ON COLUMN comissoes_calculadas.saldo_anterior IS 'Saldo negativo transferido do mês anterior (carryover)';
COMMENT ON COLUMN comissoes_calculadas.quitado IS 'Indica se a comissão foi paga/quitada';
COMMENT ON COLUMN comissoes_calculadas.data_quitacao IS 'Data em que a comissão foi quitada';
COMMENT ON COLUMN comissoes_calculadas.valor_quitado IS 'Valor efetivamente pago na quitação';
