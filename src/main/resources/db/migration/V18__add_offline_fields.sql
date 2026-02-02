-- Add offline synchronization field
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS local_id VARCHAR(255);

-- Add discount support fields
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS tipo_desconto VARCHAR(20);
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS valor_desconto DECIMAL(19, 2);

-- Add persisted totals for performance and reporting
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS valor_total_sem_desconto DECIMAL(19, 2) DEFAULT 0.00;
ALTER TABLE ordens_servico ADD COLUMN IF NOT EXISTS valor_total_com_desconto DECIMAL(19, 2) DEFAULT 0.00;

-- Update existing records to have consistent default values (optional but good practice)
UPDATE ordens_servico SET valor_total_sem_desconto = valor_total WHERE valor_total_sem_desconto = 0;
UPDATE ordens_servico SET valor_total_com_desconto = valor_total WHERE valor_total_com_desconto = 0;
