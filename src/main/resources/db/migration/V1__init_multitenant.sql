/*
* V1__init_multitenant.sql
* Esquema Inicial Consolidado (Sistema de Comissão V2 - Multi-Inquilino)
*/

-- 1. Tabela de Empresas (Tenant)
CREATE TABLE empresas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(20),
    plano VARCHAR(20) NOT NULL DEFAULT 'BRONZE', -- BRONZE, PRATA, OURO
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    data_atualizacao TIMESTAMP
);

-- 2. Tabela de Usuários (Com Vínculo de Empresa)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- ADMIN_EMPRESA, FUNCIONARIO
    active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_user_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

-- 3. Funcionalidades (Features)
CREATE TABLE features (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(100) NOT NULL UNIQUE,
    descricao VARCHAR(255),
    plano_minimo VARCHAR(20) DEFAULT 'BRONZE'
);

CREATE TABLE user_features (
    usuario_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, feature_id),
    CONSTRAINT fk_user_feature_user FOREIGN KEY (usuario_id) REFERENCES users(id),
    CONSTRAINT fk_user_feature_feature FOREIGN KEY (feature_id) REFERENCES features(id)
);

-- 4. Clientes (Por Empresa)
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    razao_social VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    cnpj VARCHAR(20) UNIQUE,
    endereco VARCHAR(255),
    contato VARCHAR(255),
    email VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ATIVO',
    -- Endereço detalhado
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2),
    cep VARCHAR(10),
    CONSTRAINT fk_cliente_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

-- 5. Catálogo de Peças/Serviços (Por Empresa)
CREATE TABLE tipos_peca (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    valor_padrao NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_tipo_peca_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

-- 6. Ordens de Serviço (OS)
CREATE TABLE ordens_servico (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    usuario_id BIGINT, -- Quem criou/vendeu
    data DATE NOT NULL,
    status VARCHAR(50) NOT NULL, -- ABERTA, FINALIZADA, CANCELADA
    valor_total NUMERIC(19, 2) DEFAULT 0,
    CONSTRAINT fk_os_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_os_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_os_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

-- 7. Veículos e Itens da OS
CREATE TABLE veiculos_servico (
    id BIGSERIAL PRIMARY KEY,
    ordem_servico_id BIGINT NOT NULL,
    placa VARCHAR(20),
    modelo VARCHAR(100),
    cor VARCHAR(50),
    valor_total NUMERIC(19, 2) DEFAULT 0,
    CONSTRAINT fk_veiculo_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id) ON DELETE CASCADE
);

CREATE TABLE pecas_servico (
    id BIGSERIAL PRIMARY KEY,
    veiculo_id BIGINT NOT NULL,
    tipo_peca_id BIGINT NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_peca_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos_servico(id) ON DELETE CASCADE,
    CONSTRAINT fk_peca_tipo FOREIGN KEY (tipo_peca_id) REFERENCES tipos_peca(id)
);

-- 8. Financeiro: Faturamento (Entradas)
CREATE TABLE faturamentos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    data_faturamento DATE NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    ordem_servico_id BIGINT UNIQUE, -- Link direto com a OS (1 pra 1)
    usuario_id BIGINT, -- Vendedor
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_faturamento_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_faturamento_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id),
    CONSTRAINT fk_faturamento_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

-- 9. Financeiro: Adiantamentos (Saídas para Funcionário)
CREATE TABLE pagamentos_adiantados (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    data_pagamento DATE NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    descricao VARCHAR(500),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_adiantamento_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_adiantamento_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

-- 10. Financeiro: Despesas Gerais (Saídas da Empresa)
CREATE TABLE despesas (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    data_despesa DATE NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    categoria VARCHAR(50) NOT NULL, -- ALUGUEL, AGUA, LUZ, INTERNET...
    descricao VARCHAR(255),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_despesa_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

-- 11. Cache de Comissões
CREATE TABLE comissoes_calculadas (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    ano_mes_referencia VARCHAR(7) NOT NULL, -- "2023-10"
    faturamento_mensal_total NUMERIC(19, 2) NOT NULL,
    faixa_comissao_descricao VARCHAR(255) NOT NULL,
    porcentagem_comissao_aplicada NUMERIC(5, 2) NOT NULL,
    valor_bruto_comissao NUMERIC(19, 2) NOT NULL,
    valor_total_adiantamentos NUMERIC(19, 2) NOT NULL,
    saldo_a_receber NUMERIC(19, 2) NOT NULL,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_comissao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_comissao_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

CREATE INDEX idx_comissao_usuario_mes ON comissoes_calculadas(usuario_id, ano_mes_referencia);
