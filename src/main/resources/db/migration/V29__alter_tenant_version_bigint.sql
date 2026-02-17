-- Alterar tenant_version para BIGINT para garantir escalabilidade
ALTER TABLE empresas ALTER COLUMN tenant_version TYPE BIGINT;
