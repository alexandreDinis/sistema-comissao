-- Add payment due date field to ordens_servico table
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS data_vencimento DATE;

-- Set default value for existing records (use creation date)
UPDATE ordens_servico SET data_vencimento = data WHERE data_vencimento IS NULL;

-- Add descricao to pecas_servico (from previous feature)
ALTER TABLE pecas_servico ADD COLUMN IF NOT EXISTS descricao VARCHAR(500);
