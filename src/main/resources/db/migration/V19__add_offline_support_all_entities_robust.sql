-- 1. Enable pgcrypto for UUID generation (if not enabled)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. CLIENTES
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS local_id VARCHAR(36);
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Backfill
UPDATE clientes SET local_id = gen_random_uuid()::text WHERE local_id IS NULL;
UPDATE clientes SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

-- Constraints
ALTER TABLE clientes ALTER COLUMN local_id SET NOT NULL;
ALTER TABLE clientes ADD CONSTRAINT unique_cliente_local_id UNIQUE (local_id);

-- 3. VEICULOS_SERVICO
ALTER TABLE veiculos_servico ADD COLUMN IF NOT EXISTS local_id VARCHAR(36);
ALTER TABLE veiculos_servico ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE veiculos_servico ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE veiculos_servico SET local_id = gen_random_uuid()::text WHERE local_id IS NULL;
UPDATE veiculos_servico SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

ALTER TABLE veiculos_servico ALTER COLUMN local_id SET NOT NULL;
ALTER TABLE veiculos_servico ADD CONSTRAINT unique_veiculo_local_id UNIQUE (local_id);

-- 4. DESPESAS
ALTER TABLE despesas ADD COLUMN IF NOT EXISTS local_id VARCHAR(36);
ALTER TABLE despesas ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE despesas ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE despesas SET local_id = gen_random_uuid()::text WHERE local_id IS NULL;
UPDATE despesas SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

ALTER TABLE despesas ALTER COLUMN local_id SET NOT NULL;
ALTER TABLE despesas ADD CONSTRAINT unique_despesa_local_id UNIQUE (local_id);

-- 5. ORDEM_SERVICO
-- V18 already added local_id but might have left nulls or no constraints
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE ordens_servico SET local_id = gen_random_uuid()::text WHERE local_id IS NULL;
UPDATE ordens_servico SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

ALTER TABLE ordens_servico ALTER COLUMN local_id SET NOT NULL;
-- Drop constraint if exists to avoid error on re-run, or use IF NOT EXISTS logic (PG lacks convenient ADD CONSTRAINT IF NOT EXISTS)
-- We assume it doesn't exist from V18 as V18 didn't add it.
ALTER TABLE ordens_servico ADD CONSTRAINT unique_os_local_id UNIQUE (local_id);
