-- V33: Adiciona campos de informações de cobrança/pagamento na tabela empresas
-- Esses campos são exibidos no PDF da Ordem de Serviço

ALTER TABLE empresas ADD COLUMN IF NOT EXISTS pix_tipo VARCHAR(20);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS pix_chave VARCHAR(255);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS banco VARCHAR(100);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS agencia VARCHAR(20);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS conta VARCHAR(30);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS tipo_conta VARCHAR(20);
