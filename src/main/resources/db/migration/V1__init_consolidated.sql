/*
 * V1__init_consolidated.sql
 * Esquema Consolidado - Sistema de Comissão + Módulo Financeiro
 * Versão: 2.0
 * Data: 2026-01-18
 * 
 * Este schema inclui:
 * - Multi-Tenancy (Empresas)
 * - Usuários e Permissões
 * - Clientes e Catálogo
 * - Ordens de Serviço
 * - Financeiro (Faturamento, Despesas, Adiantamentos)
 * - Regras de Comissão Flexíveis
 * - **NOVO** Contas a Pagar e Contas a Receber
 */

-- ===========================================
-- 1. EMPRESAS (TENANT)
-- ===========================================
CREATE TABLE empresas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    razao_social VARCHAR(255),
    cnpj VARCHAR(20),
    plano VARCHAR(20) NOT NULL DEFAULT 'BRONZE', -- BRONZE, PRATA, OURO
    modo_comissao VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL', -- INDIVIDUAL, COLETIVA
    logo_path VARCHAR(255),
    endereco VARCHAR(255),
    telefone VARCHAR(50),
    email VARCHAR(100),
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    data_atualizacao TIMESTAMP
);

-- ===========================================
-- 2. USUÁRIOS
-- ===========================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT, -- NULL para SUPER_ADMIN
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- SUPER_ADMIN, ADMIN_EMPRESA, FUNCIONARIO
    active BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_user_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

-- ===========================================
-- 3. FUNCIONALIDADES (FEATURES)
-- ===========================================
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

-- ===========================================
-- 4. RECUPERAÇÃO DE SENHA
-- ===========================================
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);

-- ===========================================
-- 5. CLIENTES (POR EMPRESA)
-- ===========================================
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
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2),
    cep VARCHAR(10),
    CONSTRAINT fk_cliente_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

-- ===========================================
-- 6. CATÁLOGO DE PEÇAS/SERVIÇOS
-- ===========================================
CREATE TABLE tipos_peca (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    valor_padrao NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_tipo_peca_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

-- ===========================================
-- 7. ORDENS DE SERVIÇO (OS)
-- ===========================================
CREATE TABLE ordens_servico (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    usuario_id BIGINT,
    data DATE NOT NULL,
    data_vencimento DATE,
    status VARCHAR(50) NOT NULL, -- ABERTA, FINALIZADA, CANCELADA
    valor_total NUMERIC(19, 2) DEFAULT 0,
    tipo_desconto VARCHAR(20),
    valor_desconto DECIMAL(19,2),
    valor_total_sem_desconto DECIMAL(19,2) DEFAULT 0 NOT NULL,
    valor_total_com_desconto DECIMAL(19,2) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_os_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_os_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_os_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

-- ===========================================
-- 8. VEÍCULOS E ITENS DA OS
-- ===========================================
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
    descricao VARCHAR(500),
    CONSTRAINT fk_peca_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos_servico(id) ON DELETE CASCADE,
    CONSTRAINT fk_peca_tipo FOREIGN KEY (tipo_peca_id) REFERENCES tipos_peca(id)
);

-- ===========================================
-- 9. FINANCEIRO: FATURAMENTO (RECEITAS - COMPETÊNCIA)
-- ===========================================
CREATE TABLE faturamentos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    data_faturamento DATE NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    ordem_servico_id BIGINT UNIQUE,
    usuario_id BIGINT,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_faturamento_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_faturamento_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id),
    CONSTRAINT fk_faturamento_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

-- ===========================================
-- 10. FINANCEIRO: ADIANTAMENTOS
-- ===========================================
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

-- ===========================================
-- 11. FINANCEIRO: DESPESAS (SAÍDAS - COMPETÊNCIA)
-- ===========================================
CREATE TABLE despesas (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    data_despesa DATE NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    descricao VARCHAR(255),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_despesa_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

-- ===========================================
-- 12. COMISSÕES CALCULADAS (CACHE)
-- ===========================================
CREATE TABLE comissoes_calculadas (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    usuario_id BIGINT,
    ano_mes_referencia VARCHAR(7) NOT NULL,
    faturamento_mensal_total NUMERIC(19, 2) NOT NULL,
    faixa_comissao_descricao VARCHAR(255) NOT NULL,
    porcentagem_comissao_aplicada NUMERIC(5, 2) NOT NULL,
    valor_bruto_comissao NUMERIC(19, 2) NOT NULL,
    valor_total_adiantamentos NUMERIC(19, 2) NOT NULL,
    saldo_a_receber NUMERIC(19, 2) NOT NULL,
    saldo_anterior DECIMAL(19,2) DEFAULT 0,
    quitado BOOLEAN DEFAULT FALSE,
    data_quitacao TIMESTAMP,
    valor_quitado DECIMAL(19,2),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_comissao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_comissao_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

CREATE INDEX idx_comissao_usuario_mes ON comissoes_calculadas(usuario_id, ano_mes_referencia);
CREATE INDEX idx_comissao_empresa_mes ON comissoes_calculadas(empresa_id, ano_mes_referencia);

-- ===========================================
-- 13. REGRAS DE COMISSÃO (POR EMPRESA)
-- ===========================================
CREATE TABLE regras_comissao (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    tipo_regra VARCHAR(50) NOT NULL, -- FAIXA_FATURAMENTO, FIXA_FUNCIONARIO, FIXA_EMPRESA, HIBRIDA
    ativo BOOLEAN DEFAULT TRUE,
    descricao VARCHAR(500),
    percentual_fixo NUMERIC(5, 2),
    data_inicio DATE NOT NULL,
    data_fim DATE,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_regra_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

CREATE INDEX idx_regra_empresa_ativo ON regras_comissao(empresa_id, ativo);

-- ===========================================
-- 14. FAIXAS DE COMISSÃO
-- ===========================================
CREATE TABLE faixas_comissao_config (
    id BIGSERIAL PRIMARY KEY,
    regra_id BIGINT NOT NULL,
    min_faturamento NUMERIC(19, 2) NOT NULL,
    max_faturamento NUMERIC(19, 2),
    porcentagem NUMERIC(5, 2) NOT NULL,
    descricao VARCHAR(255),
    ordem INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_faixa_regra FOREIGN KEY (regra_id) REFERENCES regras_comissao(id) ON DELETE CASCADE
);

CREATE INDEX idx_faixa_regra ON faixas_comissao_config(regra_id, ordem);

-- ===========================================
-- 15. COMISSÃO FIXA POR FUNCIONÁRIO
-- ===========================================
CREATE TABLE comissao_fixa_funcionario (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    porcentagem NUMERIC(5, 2) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_comissao_fixa_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_comissao_fixa_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

CREATE INDEX idx_comissao_fixa_usuario ON comissao_fixa_funcionario(usuario_id, ativo);
CREATE INDEX idx_comissao_fixa_empresa ON comissao_fixa_funcionario(empresa_id, ativo);

-- ===========================================
-- 16. SALÁRIO/REMUNERAÇÃO POR FUNCIONÁRIO
-- ===========================================
CREATE TABLE salario_funcionario (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    tipo_remuneracao VARCHAR(20) NOT NULL DEFAULT 'COMISSAO', -- COMISSAO, SALARIO_FIXO, MISTA
    salario_base NUMERIC(19, 2),
    percentual_comissao NUMERIC(5, 2),
    ativo BOOLEAN DEFAULT TRUE,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_salario_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_salario_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

CREATE INDEX idx_salario_funcionario ON salario_funcionario(usuario_id, ativo);
CREATE INDEX idx_salario_empresa ON salario_funcionario(empresa_id, ativo);

-- ===========================================
-- 17. CONTAS A PAGAR (NOVO - MÓDULO FINANCEIRO)
-- ===========================================
CREATE TABLE contas_pagar (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    descricao VARCHAR(255),
    valor DECIMAL(19,2) NOT NULL,
    data_competencia DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, PAGO, CANCELADO
    tipo VARCHAR(30) NOT NULL, -- DESPESA_OPERACIONAL, COMISSAO_FUNCIONARIO, ADIANTAMENTO, SALARIO, FORNECEDOR, IMPOSTO, OUTROS
    meio_pagamento VARCHAR(30), -- DINHEIRO, PIX, CARTAO_CREDITO, CARTAO_DEBITO, BOLETO, TRANSFERENCIA, CHEQUE
    numero_parcela INT,
    total_parcelas INT,
    parcela_origem_id BIGINT,
    despesa_id BIGINT,
    comissao_id BIGINT,
    funcionario_id BIGINT,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_conta_pagar_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_conta_pagar_despesa FOREIGN KEY (despesa_id) REFERENCES despesas(id),
    CONSTRAINT fk_conta_pagar_comissao FOREIGN KEY (comissao_id) REFERENCES comissoes_calculadas(id),
    CONSTRAINT fk_conta_pagar_funcionario FOREIGN KEY (funcionario_id) REFERENCES users(id),
    CONSTRAINT fk_conta_pagar_parcela_origem FOREIGN KEY (parcela_origem_id) REFERENCES contas_pagar(id)
);

CREATE INDEX idx_contas_pagar_empresa_status ON contas_pagar(empresa_id, status);
CREATE INDEX idx_contas_pagar_vencimento ON contas_pagar(data_vencimento);
CREATE INDEX idx_contas_pagar_competencia ON contas_pagar(data_competencia);

-- ===========================================
-- 18. CONTAS A RECEBER (NOVO - MÓDULO FINANCEIRO)
-- ===========================================
CREATE TABLE contas_receber (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    descricao VARCHAR(255),
    valor DECIMAL(19,2) NOT NULL,
    data_competencia DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    data_recebimento DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, PAGO, CANCELADO
    tipo VARCHAR(30) NOT NULL, -- ORDEM_SERVICO, VENDA_DIRETA, OUTROS
    meio_pagamento VARCHAR(30), -- DINHEIRO, PIX, CARTAO_CREDITO, CARTAO_DEBITO, BOLETO, TRANSFERENCIA, CHEQUE
    faturamento_id BIGINT,
    ordem_servico_id BIGINT,
    cliente_id BIGINT,
    funcionario_responsavel_id BIGINT,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_conta_receber_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_conta_receber_faturamento FOREIGN KEY (faturamento_id) REFERENCES faturamentos(id),
    CONSTRAINT fk_conta_receber_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico(id),
    CONSTRAINT fk_conta_receber_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_conta_receber_funcionario FOREIGN KEY (funcionario_responsavel_id) REFERENCES users(id)
);

CREATE INDEX idx_contas_receber_empresa_status ON contas_receber(empresa_id, status);
CREATE INDEX idx_contas_receber_vencimento ON contas_receber(data_vencimento);
CREATE INDEX idx_contas_receber_recebimento ON contas_receber(data_recebimento);
CREATE INDEX idx_contas_receber_competencia ON contas_receber(data_competencia);
CREATE INDEX idx_contas_receber_funcionario ON contas_receber(funcionario_responsavel_id, status);

-- Comentários para documentação
COMMENT ON TABLE contas_pagar IS 'Controle de contas a pagar (saídas de caixa)';
COMMENT ON TABLE contas_receber IS 'Controle de contas a receber (entradas de caixa) - Base para cálculo de comissão';
COMMENT ON COLUMN contas_pagar.data_competencia IS 'Data em que a despesa PERTENCE contabilmente';
COMMENT ON COLUMN contas_pagar.data_vencimento IS 'Data em que a conta DEVE ser paga';
COMMENT ON COLUMN contas_pagar.data_pagamento IS 'Data em que a conta FOI paga (fluxo de caixa)';
COMMENT ON COLUMN contas_receber.data_competencia IS 'Data em que a receita PERTENCE contabilmente';
COMMENT ON COLUMN contas_receber.data_vencimento IS 'Data em que a conta DEVE ser recebida';
COMMENT ON COLUMN contas_receber.data_recebimento IS 'Data em que a conta FOI recebida (fluxo de caixa e comissão)';
