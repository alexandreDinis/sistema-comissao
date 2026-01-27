-- Adiciona campo para data de vencimento do pagamento ao prestador
ALTER TABLE pecas_servico ADD COLUMN IF NOT EXISTS data_vencimento_prestador DATE;
