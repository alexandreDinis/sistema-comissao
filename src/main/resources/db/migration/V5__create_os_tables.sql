CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    razao_social VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    cnpj VARCHAR(20) UNIQUE,
    endereco VARCHAR(255),
    contato VARCHAR(100),
    email VARCHAR(100)
);

CREATE TABLE tipos_peca (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    valor_padrao NUMERIC(19, 2) NOT NULL
);

CREATE TABLE ordens_servico (
    id BIGSERIAL PRIMARY KEY,
    data DATE NOT NULL,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    valor_total NUMERIC(19, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE veiculos_servico (
    id BIGSERIAL PRIMARY KEY,
    placa VARCHAR(20),
    modelo VARCHAR(100),
    cor VARCHAR(50),
    ordem_servico_id BIGINT NOT NULL REFERENCES ordens_servico(id),
    valor_total NUMERIC(19, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE pecas_servico (
    id BIGSERIAL PRIMARY KEY,
    tipo_peca_id BIGINT NOT NULL REFERENCES tipos_peca(id),
    veiculo_id BIGINT NOT NULL REFERENCES veiculos_servico(id),
    valor NUMERIC(19, 2) NOT NULL
);
