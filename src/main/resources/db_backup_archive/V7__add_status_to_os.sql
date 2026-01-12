-- Add status column to ordens_servico with default 'ABERTA'
ALTER TABLE ordens_servico
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ABERTA';

-- Add order_servico_id to faturamentos table
ALTER TABLE faturamentos
ADD COLUMN ordem_servico_id BIGINT;

-- Add Foreign Key constraint with unique constraint for OneToOne
ALTER TABLE faturamentos
ADD CONSTRAINT fk_faturamentos_ordem_servico
FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id);

ALTER TABLE faturamentos
ADD CONSTRAINT uk_faturamentos_ordem_servico UNIQUE (ordem_servico_id);
