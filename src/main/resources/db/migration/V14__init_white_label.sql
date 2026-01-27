-- 1. Tabela de Planos de Licença
CREATE TABLE planos_licenca (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    valor_mensalidade DECIMAL(10,2) NOT NULL,
    valor_por_tenant DECIMAL(10,2) NOT NULL,
    limite_tenants INT,
    limite_usuarios_por_tenant INT,
    suporte_prioritario BOOLEAN DEFAULT FALSE,
    white_label BOOLEAN DEFAULT TRUE,
    dominio_customizado BOOLEAN DEFAULT FALSE,
    ativo BOOLEAN DEFAULT TRUE,
    ordem INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO planos_licenca (nome, descricao, valor_mensalidade, valor_por_tenant, limite_tenants, ordem) VALUES
('BASIC', 'Ideal para começar', 497.00, 15.00, 20, 1),
('PRO', 'Para crescer', 997.00, 10.00, 100, 2),
('ENTERPRISE', 'Sem limites', 1997.00, 5.00, NULL, 3);

-- 2. Tabela de Licenças (Revendedores)
CREATE TABLE licencas (
    id BIGSERIAL PRIMARY KEY,
    
    -- Dados da Empresa
    razao_social VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    cnpj VARCHAR(18) UNIQUE,
    email VARCHAR(255) NOT NULL,
    telefone VARCHAR(20),
    
    -- Endereço
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2),
    cep VARCHAR(10),
    
    -- Plano Contratado (Snapshot)
    plano_tipo VARCHAR(50) NOT NULL,
    valor_mensalidade DECIMAL(10,2) NOT NULL,
    valor_por_tenant DECIMAL(10,2) NOT NULL,
    limite_tenants INT,
    
    -- White Label
    logo_url VARCHAR(500),
    cor_primaria VARCHAR(7),
    cor_secundaria VARCHAR(7),
    dominio_customizado VARCHAR(255),
    
    -- Dados Bancários
    banco VARCHAR(100),
    tipo_conta VARCHAR(20),
    agencia VARCHAR(10),
    conta VARCHAR(20),
    pix_tipo VARCHAR(20),
    pix_chave VARCHAR(255),
    
    -- Gateway
    gateway_pagamento VARCHAR(50),
    gateway_access_token TEXT,
    gateway_public_key TEXT,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    data_ativacao DATE,
    data_suspensao DATE,
    motivo_suspensao TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_licencas_cnpj ON licencas(cnpj);
CREATE INDEX idx_licencas_status ON licencas(status);

-- 3. Inserir Licença Default (System Admin)
-- Esta licença será a "dona" das empresas existentes
INSERT INTO licencas (
    razao_social, nome_fantasia, cnpj, email, telefone, 
    plano_tipo, valor_mensalidade, valor_por_tenant, limite_tenants, 
    status, data_ativacao
) VALUES (
    'Sistema Master Admin', 'Admin', '00.000.000/0001-00', 'admin@sistema.com', '000000000', 
    'ENTERPRISE', 0.00, 0.00, NULL, 
    'ATIVA', CURRENT_DATE
);

-- 4. Atualizar Tabela Empresas
-- Adicionar colunas novas
ALTER TABLE empresas ADD COLUMN licenca_id BIGINT;
ALTER TABLE empresas ADD COLUMN valor_mensal_pago DECIMAL(10,2);
ALTER TABLE empresas ADD COLUMN status VARCHAR(20) DEFAULT 'ATIVA';

-- Vincular empresas existentes à licença default (ID 1)
UPDATE empresas SET licenca_id = 1 WHERE licenca_id IS NULL;

-- Agora tornar obrigatório
ALTER TABLE empresas ALTER COLUMN licenca_id SET NOT NULL;
ALTER TABLE empresas ADD CONSTRAINT fk_empresa_licenca FOREIGN KEY (licenca_id) REFERENCES licencas(id);

CREATE INDEX idx_empresa_licenca ON empresas(licenca_id);
