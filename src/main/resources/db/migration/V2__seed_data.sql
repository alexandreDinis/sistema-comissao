-- 1. Inserir Empresa Padrão (OURO)
INSERT INTO empresas (nome, cnpj, plano) 
VALUES ('Empresa Padrão', '00000000000191', 'OURO');

-- 2. Inserir Features do Sistema
INSERT INTO features (codigo, descricao, plano_minimo) VALUES 
('DASHBOARD_VIEW', 'Visualizar Dashboard', 'BRONZE'),
('RELATORIO_COMISSAO_VIEW', 'Visualizar Relatório de Comissão', 'BRONZE'),
('RELATORIO_FINANCEIRO_VIEW', 'Visualizar Relatório Financeiro', 'PRATA'),
('OS_READ', 'Listar Ordens de Serviço', 'BRONZE'),
('OS_CREATE', 'Criar Ordem de Serviço', 'BRONZE'),
('OS_UPDATE', 'Editar Ordem de Serviço', 'BRONZE'),
('OS_FINALIZE', 'Finalizar Ordem de Serviço', 'BRONZE'),
('OS_CANCEL', 'Cancelar Ordem de Serviço', 'PRATA'),
('CLIENTE_READ', 'Listar Clientes', 'BRONZE'),
('CLIENTE_WRITE', 'Gerenciar Clientes', 'BRONZE'),
('PRODUTO_READ', 'Listar Catálogo', 'BRONZE'),
('PRODUTO_WRITE', 'Gerenciar Catálogo', 'BRONZE'),
('ADMIN_USERS_READ', 'Listar Usuários', 'PRATA'),
('ADMIN_USERS_WRITE', 'Gerenciar Usuários e Permissões', 'OURO'),
('ADMIN_CONFIG', 'Configurações da Empresa', 'OURO');

-- 3. Inserir Usuário Admin
-- Senha 'password' (BCrypt)
INSERT INTO users (empresa_id, email, password, role, active) 
VALUES (
    (SELECT id FROM empresas WHERE nome = 'Empresa Padrão'), 
    'admin@empresa.com', 
    '$2a$10$dxW7.k.v7F.V7j7.V7j7.e7J7.V7j7.V7j7.V7j7.V7j7.V7j7', -- password placeholder hash
    'ADMIN_EMPRESA',
    true
);

-- 4. Dar Todas as Permissões para o Admin
INSERT INTO user_features (usuario_id, feature_id)
SELECT u.id, f.id
FROM users u, features f
WHERE u.email = 'admin@empresa.com';

-- 5. Inserir Cliente de Teste
INSERT INTO clientes (empresa_id, razao_social, nome_fantasia, cnpj, contato, email, status)
VALUES (
    (SELECT id FROM empresas WHERE nome = 'Empresa Padrão'),
    'Cliente Teste LTDA', 
    'Cliente Teste', 
    '12345678000199', 
    'Fulano', 
    'contato@teste.com', 
    'ATIVO'
);

-- 6. Inserir Tipos de Peças (Catálogo)
INSERT INTO tipos_peca (empresa_id, nome, valor_padrao) VALUES 
((SELECT id FROM empresas WHERE nome = 'Empresa Padrão'), 'Limpeza Simples', 50.00),
((SELECT id FROM empresas WHERE nome = 'Empresa Padrão'), 'Polimento', 150.00),
((SELECT id FROM empresas WHERE nome = 'Empresa Padrão'), 'Higienização Interna', 120.00);
