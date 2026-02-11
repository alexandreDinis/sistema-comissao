-- 1. Garantir que PLACA não seja nula
ALTER TABLE veiculos_servico ALTER COLUMN placa set NOT NULL;

-- 2. Limpeza de duplicatas (mantém o mais recente)
DELETE FROM veiculos_servico a USING veiculos_servico b
WHERE a.id < b.id
AND a.ordem_servico_id = b.ordem_servico_id
AND a.placa = b.placa;

-- 3. Adicionar constraint UNIQUE
ALTER TABLE veiculos_servico 
ADD CONSTRAINT uk_veiculo_os_placa UNIQUE (ordem_servico_id, placa);
