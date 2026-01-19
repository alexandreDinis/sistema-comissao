-- V4: Adiciona campos de configuração tributária na tabela empresas
-- Permite que cada empresa configure sua alíquota de imposto e regime tributário

ALTER TABLE empresas ADD COLUMN IF NOT EXISTS aliquota_imposto DECIMAL(5,4) DEFAULT 0.0600;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS regime_tributario VARCHAR(30) DEFAULT 'SIMPLES_NACIONAL';
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS uf VARCHAR(2);

-- Comentário: A alíquota padrão é 6% (0.06) referente à primeira faixa do Simples Nacional Anexo III
-- A alíquota é armazenada em formato decimal (ex: 0.0600 = 6%, 0.1133 = 11.33%)
