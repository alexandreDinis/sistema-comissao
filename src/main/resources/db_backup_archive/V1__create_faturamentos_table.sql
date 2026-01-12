-- V1__initial_schema.sql

-- Tabela faturamentos
CREATE TABLE IF NOT EXISTS faturamentos (
    id BIGSERIAL PRIMARY KEY,
    data_faturamento DATE NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_faturamento_data ON faturamentos(data_faturamento);

-- Tabela pagamentos_adiantados
CREATE TABLE IF NOT EXISTS pagamentos_adiantados (
    id BIGSERIAL PRIMARY KEY,
    data_pagamento DATE NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    descricao VARCHAR(500),
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_adiantamento_data ON pagamentos_adiantados(data_pagamento);

-- Tabela comissoes_calculadas
CREATE TABLE IF NOT EXISTS comissoes_calculadas (
    id BIGSERIAL PRIMARY KEY,
    ano_mes_referencia VARCHAR(7) UNIQUE NOT NULL,
    faturamento_mensal_total DECIMAL(19, 2) NOT NULL,
    faixa_comissao_descricao VARCHAR(255) NOT NULL,
    porcentagem_comissao_aplicada DECIMAL(5, 2) NOT NULL,
    valor_bruto_comissao DECIMAL(19, 2) NOT NULL,
    valor_total_adiantamentos DECIMAL(19, 2) NOT NULL,
    saldo_a_receber DECIMAL(19, 2) NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comissao_ano_mes ON comissoes_calculadas(ano_mes_referencia);