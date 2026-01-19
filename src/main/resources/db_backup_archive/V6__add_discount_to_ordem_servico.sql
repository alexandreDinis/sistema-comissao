-- Add discount support to Service Orders
-- Migration: V6__add_discount_to_ordem_servico.sql

ALTER TABLE ordens_servico 
ADD COLUMN tipo_desconto VARCHAR(20),
ADD COLUMN valor_desconto DECIMAL(19,2),
ADD COLUMN valor_total_sem_desconto DECIMAL(19,2) DEFAULT 0 NOT NULL,
ADD COLUMN valor_total_com_desconto DECIMAL(19,2) DEFAULT 0 NOT NULL;

-- Migrate existing data: set new fields to current valorTotal
UPDATE ordens_servico 
SET valor_total_sem_desconto = valor_total,
    valor_total_com_desconto = valor_total
WHERE valor_total_sem_desconto = 0;

-- Add comment
COMMENT ON COLUMN ordens_servico.tipo_desconto IS 'Tipo de desconto: PERCENTUAL ou VALOR_FIXO';
COMMENT ON COLUMN ordens_servico.valor_desconto IS 'Valor do desconto (% ou R$)';
COMMENT ON COLUMN ordens_servico.valor_total_sem_desconto IS 'Valor total antes do desconto';
COMMENT ON COLUMN ordens_servico.valor_total_com_desconto IS 'Valor total final com desconto aplicado';
