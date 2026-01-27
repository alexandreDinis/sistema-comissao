ALTER TABLE clientes ADD COLUMN cpf VARCHAR(14);
ALTER TABLE clientes ADD COLUMN tipo_pessoa VARCHAR(20) DEFAULT 'JURIDICA';

-- Atualizar registros existentes
UPDATE clientes SET tipo_pessoa = 'JURIDICA' WHERE tipo_pessoa IS NULL;

-- Adicionar Unique Constraint para CPF + Empresa
ALTER TABLE clientes ADD CONSTRAINT uk_clientes_cpf_empresa UNIQUE (cpf, empresa_id);
