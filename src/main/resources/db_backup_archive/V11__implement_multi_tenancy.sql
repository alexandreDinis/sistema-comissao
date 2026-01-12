-- 1. Create table Empresas
CREATE TABLE empresas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(20),
    plano VARCHAR(20) NOT NULL DEFAULT 'BRONZE', -- BRONZE, PRATA, OURO
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    data_atualizacao TIMESTAMP
);

-- 2. Create Default Company and Features
INSERT INTO empresas (nome, plano) VALUES ('Empresa Padrão', 'OURO');

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

-- 3. Add empresa_id to Users
ALTER TABLE users ADD COLUMN empresa_id BIGINT;
UPDATE users SET empresa_id = (SELECT id FROM empresas WHERE nome = 'Empresa Padrão');
ALTER TABLE users ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_user_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id);

-- 4. Add empresa_id to Business Entities

-- Clientes
ALTER TABLE clientes ADD COLUMN empresa_id BIGINT;
UPDATE clientes SET empresa_id = (SELECT id FROM empresas WHERE nome = 'Empresa Padrão');
ALTER TABLE clientes ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE clientes ADD CONSTRAINT fk_cliente_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id);

-- Tipos Peca (Catálogo)
ALTER TABLE tipos_peca ADD COLUMN empresa_id BIGINT;
UPDATE tipos_peca SET empresa_id = (SELECT id FROM empresas WHERE nome = 'Empresa Padrão');
ALTER TABLE tipos_peca ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE tipos_peca ADD CONSTRAINT fk_tipo_peca_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id);

-- Ordens Servico
ALTER TABLE ordens_servico ADD COLUMN empresa_id BIGINT;
UPDATE ordens_servico SET empresa_id = (SELECT id FROM empresas WHERE nome = 'Empresa Padrão');
ALTER TABLE ordens_servico ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE ordens_servico ADD CONSTRAINT fk_os_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id);

-- Faturamentos
ALTER TABLE faturamentos ADD COLUMN empresa_id BIGINT;
UPDATE faturamentos SET empresa_id = (SELECT id FROM empresas WHERE nome = 'Empresa Padrão');
ALTER TABLE faturamentos ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE faturamentos ADD CONSTRAINT fk_faturamento_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id);

-- Pagamentos Adiantados
ALTER TABLE pagamentos_adiantados ADD COLUMN empresa_id BIGINT;
UPDATE pagamentos_adiantados SET empresa_id = (SELECT id FROM empresas WHERE nome = 'Empresa Padrão');
ALTER TABLE pagamentos_adiantados ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE pagamentos_adiantados ADD CONSTRAINT fk_adiantamento_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id);

-- Comissoes Calculadas
ALTER TABLE comissoes_calculadas ADD COLUMN empresa_id BIGINT;
UPDATE comissoes_calculadas SET empresa_id = (SELECT id FROM empresas WHERE nome = 'Empresa Padrão');
ALTER TABLE comissoes_calculadas ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE comissoes_calculadas ADD CONSTRAINT fk_comissao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id);

-- Despesas
ALTER TABLE despesas ADD COLUMN empresa_id BIGINT;
UPDATE despesas SET empresa_id = (SELECT id FROM empresas WHERE nome = 'Empresa Padrão');
ALTER TABLE despesas ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE despesas ADD CONSTRAINT fk_despesa_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id);
