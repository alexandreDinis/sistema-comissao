-- V5: Adiciona suporte a Prestadores de Serviço Externos

-- Tabela de Prestadores
CREATE TABLE IF NOT EXISTS prestadores (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(20),
    chave_pix VARCHAR(255),
    ativo BOOLEAN DEFAULT TRUE,
    empresa_id BIGINT REFERENCES empresas(id)
);

-- Campos na tabela pecas_servico para suportar terceirização
ALTER TABLE pecas_servico ADD COLUMN IF NOT EXISTS tipo_execucao VARCHAR(20) DEFAULT 'INTERNO';
ALTER TABLE pecas_servico ADD COLUMN IF NOT EXISTS prestador_id BIGINT REFERENCES prestadores(id);
ALTER TABLE pecas_servico ADD COLUMN IF NOT EXISTS custo_prestador DECIMAL(19,2);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_prestadores_empresa ON prestadores(empresa_id);
CREATE INDEX IF NOT EXISTS idx_pecas_servico_prestador ON pecas_servico(prestador_id);
