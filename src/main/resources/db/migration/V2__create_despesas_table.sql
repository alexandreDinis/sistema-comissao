-- V2__create_despesas_table.sql

CREATE TABLE IF NOT EXISTS despesas (
    id BIGSERIAL PRIMARY KEY,
    data_despesa DATE NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    descricao VARCHAR(255),
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_despesinha_data ON despesas(data_despesa);
CREATE INDEX IF NOT EXISTS idx_despesinha_categoria ON despesas(categoria);
