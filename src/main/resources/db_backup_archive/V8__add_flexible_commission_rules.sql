/*
 * V8__add_flexible_commission_rules.sql
 * Sistema de Regras de Comissionamento Flexíveis
 * 
 * Este migration adiciona suporte para:
 * - Regras de comissão configuráveis por empresa
 * - Faixas de comissão personalizadas
 * - Comissão fixa por funcionário
 * - Salário fixo ou misto para funcionários
 */

-- 1. Tabela de Regras de Comissão (por empresa)
CREATE TABLE regras_comissao (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    tipo_regra VARCHAR(50) NOT NULL, -- FAIXA_FATURAMENTO, FIXA_FUNCIONARIO, FIXA_EMPRESA, HIBRIDA
    ativo BOOLEAN DEFAULT TRUE,
    descricao VARCHAR(500),
    percentual_fixo NUMERIC(5, 2), -- Para FIXA_EMPRESA
    data_inicio DATE NOT NULL,
    data_fim DATE, -- NULL = sem fim
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_regra_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

CREATE INDEX idx_regra_empresa_ativo ON regras_comissao(empresa_id, ativo);

-- 2. Tabela de Faixas de Comissão (vinculada a uma regra)
CREATE TABLE faixas_comissao_config (
    id BIGSERIAL PRIMARY KEY,
    regra_id BIGINT NOT NULL,
    min_faturamento NUMERIC(19, 2) NOT NULL,
    max_faturamento NUMERIC(19, 2), -- NULL = sem limite superior
    porcentagem NUMERIC(5, 2) NOT NULL,
    descricao VARCHAR(255),
    ordem INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_faixa_regra FOREIGN KEY (regra_id) REFERENCES regras_comissao(id) ON DELETE CASCADE
);

CREATE INDEX idx_faixa_regra ON faixas_comissao_config(regra_id, ordem);

-- 3. Tabela de Comissão Fixa por Funcionário (override individual)
CREATE TABLE comissao_fixa_funcionario (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    porcentagem NUMERIC(5, 2) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    data_inicio DATE NOT NULL,
    data_fim DATE, -- NULL = sem fim
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_comissao_fixa_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_comissao_fixa_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

CREATE INDEX idx_comissao_fixa_usuario ON comissao_fixa_funcionario(usuario_id, ativo);
CREATE INDEX idx_comissao_fixa_empresa ON comissao_fixa_funcionario(empresa_id, ativo);

-- 4. Tabela de Salário/Remuneração por Funcionário
CREATE TABLE salario_funcionario (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    tipo_remuneracao VARCHAR(20) NOT NULL DEFAULT 'COMISSAO', -- COMISSAO, SALARIO_FIXO, MISTA
    salario_base NUMERIC(19, 2), -- Valor fixo mensal (se aplicável)
    percentual_comissao NUMERIC(5, 2), -- % de comissão adicional (para MISTA)
    ativo BOOLEAN DEFAULT TRUE,
    data_inicio DATE NOT NULL,
    data_fim DATE, -- NULL = sem fim
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_salario_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_salario_usuario FOREIGN KEY (usuario_id) REFERENCES users(id)
);

CREATE INDEX idx_salario_funcionario ON salario_funcionario(usuario_id, ativo);
CREATE INDEX idx_salario_empresa ON salario_funcionario(empresa_id, ativo);

-- 5. Migração de dados: Criar regra padrão para cada empresa existente
-- com as faixas de comissão que estavam hardcoded no sistema
INSERT INTO regras_comissao (empresa_id, nome, tipo_regra, ativo, descricao, data_inicio)
SELECT 
    id,
    'Regra Padrão (Migrada)',
    'FAIXA_FATURAMENTO',
    TRUE,
    'Regra migrada automaticamente com as faixas padrão do sistema anterior',
    CURRENT_DATE
FROM empresas
WHERE ativo = TRUE;

-- 6. Inserir as faixas padrão para cada regra criada
-- Faixa 1: R$ 0,00 a R$ 19.999,99 = 12%
INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 0.00, 19999.99, 12.00, 'Até R$ 19.999,99', 1
FROM regras_comissao WHERE nome = 'Regra Padrão (Migrada)';

-- Faixa 2: R$ 20.000,00 a R$ 24.999,99 = 15%
INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 20000.00, 24999.99, 15.00, 'R$ 20.000,00 a R$ 24.999,99', 2
FROM regras_comissao WHERE nome = 'Regra Padrão (Migrada)';

-- Faixa 3: R$ 25.000,00 a R$ 29.999,99 = 18%
INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 25000.00, 29999.99, 18.00, 'R$ 25.000,00 a R$ 29.999,99', 3
FROM regras_comissao WHERE nome = 'Regra Padrão (Migrada)';

-- Faixa 4: R$ 30.000,00 a R$ 34.999,99 = 20%
INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 30000.00, 34999.99, 20.00, 'R$ 30.000,00 a R$ 34.999,99', 4
FROM regras_comissao WHERE nome = 'Regra Padrão (Migrada)';

-- Faixa 5: Acima de R$ 35.000,00 = 25%
INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 35000.00, NULL, 25.00, 'Acima de R$ 35.000,00', 5
FROM regras_comissao WHERE nome = 'Regra Padrão (Migrada)';

-- 7. Criar configuração COMISSAO padrão para todos os funcionários existentes
INSERT INTO salario_funcionario (empresa_id, usuario_id, tipo_remuneracao, ativo, data_inicio)
SELECT 
    u.empresa_id,
    u.id,
    'COMISSAO',
    TRUE,
    CURRENT_DATE
FROM users u
WHERE u.empresa_id IS NOT NULL 
  AND u.active = TRUE;
