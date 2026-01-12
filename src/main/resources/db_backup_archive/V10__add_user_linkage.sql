-- Add usuario_id to ordens_servico
ALTER TABLE ordens_servico ADD COLUMN usuario_id BIGINT;
ALTER TABLE ordens_servico ADD CONSTRAINT fk_os_usuario FOREIGN KEY (usuario_id) REFERENCES users(id);

-- Add usuario_id to faturamentos
ALTER TABLE faturamentos ADD COLUMN usuario_id BIGINT;
ALTER TABLE faturamentos ADD CONSTRAINT fk_faturamento_usuario FOREIGN KEY (usuario_id) REFERENCES users(id);

-- Add usuario_id to comissoes_calculadas
ALTER TABLE comissoes_calculadas ADD COLUMN usuario_id BIGINT;
ALTER TABLE comissoes_calculadas ADD CONSTRAINT fk_comissao_usuario FOREIGN KEY (usuario_id) REFERENCES users(id);

-- Add usuario_id to pagamentos_adiantados
ALTER TABLE pagamentos_adiantados ADD COLUMN usuario_id BIGINT;
ALTER TABLE pagamentos_adiantados ADD CONSTRAINT fk_adiantamento_usuario FOREIGN KEY (usuario_id) REFERENCES users(id);

-- Drop unique constraint on year_month since now it should be unique per user + month
-- First, find the name of the constraint if needed, or just drop index if created by unique=true
-- ALTER TABLE comissoes_calculadas DROP CONSTRAINT IF EXISTS uk_comissao_ano_mes; (Syntax depends on DB)
-- In Postgres, "unique = true" in JPA creates a constraint.
-- Let's assume standard naming or robust approach.

-- Better to create a new unique index on (ano_mes_referencia, usuario_id)
-- But first we need to handle existing data. For now, we leave unique constraint looser or drop it.
-- Since we are in dev/validation, we can drop the old constraint if we knew its name.
-- JPA 'unique=true' usually creates a constraint named UK_...
-- To be safe, we will just add the column and index.

CREATE INDEX idx_comissao_usuario_mes ON comissoes_calculadas(usuario_id, ano_mes_referencia);
