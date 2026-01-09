-- Adicionar colunas de endereço e status na tabela clientes (Compatible with H2)

ALTER TABLE clientes ADD COLUMN status VARCHAR(20) DEFAULT 'ATIVO' NOT NULL;
ALTER TABLE clientes ADD COLUMN logradouro VARCHAR(255);
ALTER TABLE clientes ADD COLUMN numero VARCHAR(20);
ALTER TABLE clientes ADD COLUMN complemento VARCHAR(255);
ALTER TABLE clientes ADD COLUMN bairro VARCHAR(100);
ALTER TABLE clientes ADD COLUMN cidade VARCHAR(100);
ALTER TABLE clientes ADD COLUMN estado VARCHAR(2);
ALTER TABLE clientes ADD COLUMN cep VARCHAR(10);
