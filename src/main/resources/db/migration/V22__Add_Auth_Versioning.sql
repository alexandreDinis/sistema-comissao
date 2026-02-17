-- Adicionar colunas de versionamento
ALTER TABLE users ADD COLUMN auth_version INT DEFAULT 0 NOT NULL;
ALTER TABLE empresas ADD COLUMN tenant_version INT DEFAULT 0 NOT NULL;

-- Índices para lookup ultra-rápido (cache miss fallback)
CREATE INDEX idx_users_auth_version ON users(id, auth_version, active);
CREATE INDEX idx_empresas_tenant_version ON empresas(id, tenant_version, status);

-- Opcional: índice para invalidação em massa (se precisar)
CREATE INDEX idx_users_empresa_id ON users(empresa_id);
