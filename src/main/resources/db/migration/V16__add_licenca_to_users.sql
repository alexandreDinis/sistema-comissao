-- Adiciona coluna licenca_id na tabela users
ALTER TABLE users ADD COLUMN licenca_id BIGINT;

-- Adiciona a constraint de Foreign Key
ALTER TABLE users 
ADD CONSTRAINT fk_users_licenca 
FOREIGN KEY (licenca_id) REFERENCES licencas(id);

-- Cria índice para performance
CREATE INDEX idx_users_licenca ON users(licenca_id);
