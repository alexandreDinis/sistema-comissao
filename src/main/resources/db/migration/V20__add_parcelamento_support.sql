-- Migration: Adicionar suporte a parcelamento de compras no cartão de crédito

-- Adicionar campos de parcelamento na tabela despesas
ALTER TABLE despesas ADD COLUMN IF NOT EXISTS parcelado BOOLEAN DEFAULT FALSE;
ALTER TABLE despesas ADD COLUMN IF NOT EXISTS numero_parcelas INTEGER;
ALTER TABLE despesas ADD COLUMN IF NOT EXISTS parcela_atual INTEGER;
ALTER TABLE despesas ADD COLUMN IF NOT EXISTS despesa_pai_id BIGINT;

-- Adicionar foreign key para vincular parcelas à despesa original
ALTER TABLE despesas ADD CONSTRAINT fk_despesas_despesa_pai 
    FOREIGN KEY (despesa_pai_id) REFERENCES despesas(id) ON DELETE CASCADE;

-- Adicionar índice para melhorar performance de consultas de parcelas
CREATE INDEX IF NOT EXISTS idx_despesas_parcelado ON despesas(parcelado);
CREATE INDEX IF NOT EXISTS idx_despesas_despesa_pai ON despesas(despesa_pai_id);

-- Comentários para documentação
COMMENT ON COLUMN despesas.parcelado IS 'Indica se a despesa é parcelada';
COMMENT ON COLUMN despesas.numero_parcelas IS 'Número total de parcelas (ex: 12 para 12x)';
COMMENT ON COLUMN despesas.parcela_atual IS 'Número da parcela atual (ex: 1 de 12)';
COMMENT ON COLUMN despesas.despesa_pai_id IS 'ID da despesa original (primeira parcela) para vincular todas as parcelas';
