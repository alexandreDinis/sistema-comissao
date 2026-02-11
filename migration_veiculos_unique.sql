-- 1. Garantir que PLACA não seja nula (requisito de negócio e integridade)
ALTER TABLE veiculos_servico 
ALTER COLUMN placa set NOT NULL;

-- 2. Limpeza de duplicatas existente (se houver)
-- Mantém o registro mais recente (maior ID ou updated_at) para cada par (OS, Placa)
DELETE FROM veiculos_servico a USING veiculos_servico b
WHERE a.id < b.id
AND a.ordem_servico_id = b.ordem_servico_id
AND a.placa = b.placa;

-- 3. Adicionar constraint UNIQUE para evitar novas duplicatas
ALTER TABLE veiculos_servico 
ADD CONSTRAINT uk_veiculo_os_placa UNIQUE (ordem_servico_id, placa);
