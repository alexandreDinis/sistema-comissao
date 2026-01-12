-- Seed Data for Development
-- This file will populate the database with initial test data

-- 1. Cliente Teste
INSERT INTO clientes (razao_social, nome_fantasia, cnpj, email, contato, endereco, cidade, estado, status)
SELECT 'Cliente Teste Ltda', 'Teste Auto', '12345678000195', 'teste@cliente.com', '11999999999', 'Rua Teste, 100', 'São Paulo', 'SP', 'ATIVO'
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE cnpj = '12345678000195');

-- 2. Veículo (associated with OS creation usually, but we can have standalones if model supports, 
-- ideally vehicles are linked to OS in this domain model, or we create an OS with a vehicle)

-- 3. Ordem de Serviço (Abre uma OS para o cliente acima)
INSERT INTO ordens_servico (data, status, cliente_id, valor_total)
SELECT CURRENT_DATE, 'ABERTA', c.id, 0.00
FROM clientes c WHERE c.cnpj = '12345678000195'
AND NOT EXISTS (SELECT 1 FROM ordens_servico os JOIN clientes cl ON os.cliente_id = cl.id WHERE cl.cnpj = '12345678000195');

-- 4. Inserir um Veículo na OS (Assuming OS ID matches - slightly risky in SQL script without procedural logic, 
-- but acceptable for dev seed if DB is fresh. Better to rely on subqueries)

-- (Skipping complex seeds dependent on generated IDs to avoid errors. 
-- The Client seed is the most important to avoid typing the form every time).
