-- V7: Alterar constraint de CNPJ do Cliente para ser por Empresa (Multi-tenant)
-- Antes: CNPJ único globalmente
-- Depois: CNPJ único por empresa

-- 1. Remover constraint de unicidade global do CNPJ (se existir)
-- O nome da constraint pode variar dependendo de como foi criada
ALTER TABLE clientes DROP CONSTRAINT IF EXISTS clientes_cnpj_key;
ALTER TABLE clientes DROP CONSTRAINT IF EXISTS uk_cliente_cnpj;
ALTER TABLE clientes DROP CONSTRAINT IF EXISTS clientes_cnpj_unique;

-- Tentar remover índice único se existir
DROP INDEX IF EXISTS clientes_cnpj_key;
DROP INDEX IF EXISTS uk_cliente_cnpj;

-- 2. Criar nova constraint composta (CNPJ + Empresa)
-- Isso permite o mesmo CNPJ em empresas diferentes
ALTER TABLE clientes ADD CONSTRAINT uk_cliente_cnpj_empresa UNIQUE (cnpj, empresa_id);
