/*
 * V2__seed_data.sql
 * Dados Iniciais do Sistema
 */

-- ===========================================
-- 1. FEATURES DO SISTEMA
-- ===========================================
INSERT INTO features (codigo, descricao, plano_minimo) VALUES 
('DASHBOARD_VIEW', 'Visualizar Dashboard', 'BRONZE'),
('RELATORIO_COMISSAO_VIEW', 'Visualizar Relatório de Comissão', 'BRONZE'),
('RELATORIO_FINANCEIRO_VIEW', 'Visualizar Relatório Financeiro', 'PRATA'),
('COMISSAO_EMPRESA_VIEW', 'Visualizar comissão consolidada da empresa', 'BRONZE'),
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
('ADMIN_CONFIG', 'Configurações da Empresa', 'OURO'),
-- Features de Plataforma
('PLATFORM_DASHBOARD_VIEW', 'Visualizar Dashboard da Plataforma', 'OURO'),
('PLATFORM_COMPANY_MANAGE', 'Gerenciar Empresas (SaaS)', 'OURO'),
('PLATFORM_PLAN_MANAGE', 'Gerenciar Planos (SaaS)', 'OURO'),
-- NOVAS Features do Módulo Financeiro
('FINANCEIRO_VIEW', 'Visualizar Módulo Financeiro', 'PRATA'),
('FINANCEIRO_CONTAS_PAGAR', 'Gerenciar Contas a Pagar', 'PRATA'),
('FINANCEIRO_CONTAS_RECEBER', 'Gerenciar Contas a Receber', 'PRATA'),
('FINANCEIRO_FLUXO_CAIXA', 'Visualizar Fluxo de Caixa', 'OURO')
ON CONFLICT (codigo) DO NOTHING;

-- ===========================================
-- 2. EMPRESA PADRÃO (DEMO/TESTE)
-- ===========================================
INSERT INTO empresas (nome, razao_social, cnpj, plano, modo_comissao) 
VALUES ('Empresa Padrão', 'Empresa Teste LTDA', '00000000000191', 'OURO', 'COLETIVA');

-- ===========================================
-- 3. SUPER ADMIN (PLATAFORMA)
-- ===========================================
INSERT INTO users (empresa_id, email, password, role, active, must_change_password) 
VALUES (
    NULL, -- SUPER_ADMIN não tem empresa
    'saas@plataforma.com', 
    '$2a$10$placeholderHASH...........................', 
    'SUPER_ADMIN',
    true,
    false
);

-- Dar permissões de Plataforma para o Super Admin
INSERT INTO user_features (usuario_id, feature_id)
SELECT u.id, f.id
FROM users u, features f
WHERE u.email = 'saas@plataforma.com'
AND f.codigo IN ('PLATFORM_DASHBOARD_VIEW', 'PLATFORM_COMPANY_MANAGE', 'PLATFORM_PLAN_MANAGE', 'ADMIN_USERS_WRITE', 'ADMIN_CONFIG')
ON CONFLICT DO NOTHING;

-- ===========================================
-- 4. ADMIN DA EMPRESA TESTE
-- ===========================================
INSERT INTO users (empresa_id, email, password, role, active, must_change_password) 
VALUES (
    (SELECT id FROM empresas WHERE nome = 'Empresa Padrão'), 
    'admin@empresa.com', 
    '$2a$10$dxW7.k.v7F.V7j7.V7j7.e7J7.V7j7.V7j7.V7j7.V7j7.V7j7',
    'ADMIN_EMPRESA',
    true,
    false
);

-- Dar Todas as Permissões para o Admin
INSERT INTO user_features (usuario_id, feature_id)
SELECT u.id, f.id
FROM users u, features f
WHERE u.email = 'admin@empresa.com'
ON CONFLICT DO NOTHING;

-- ===========================================
-- 5. REGRA DE COMISSÃO PADRÃO
-- ===========================================
INSERT INTO regras_comissao (empresa_id, nome, tipo_regra, ativo, descricao, data_inicio)
SELECT 
    id,
    'Regra Padrão',
    'FAIXA_FATURAMENTO',
    TRUE,
    'Regra de comissão por faixas de faturamento',
    CURRENT_DATE
FROM empresas
WHERE nome = 'Empresa Padrão';

-- Faixas de comissão padrão
INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 0.00, 19999.99, 12.00, 'Até R$ 19.999,99', 1
FROM regras_comissao WHERE nome = 'Regra Padrão';

INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 20000.00, 24999.99, 15.00, 'R$ 20.000,00 a R$ 24.999,99', 2
FROM regras_comissao WHERE nome = 'Regra Padrão';

INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 25000.00, 29999.99, 18.00, 'R$ 25.000,00 a R$ 29.999,99', 3
FROM regras_comissao WHERE nome = 'Regra Padrão';

INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 30000.00, 34999.99, 20.00, 'R$ 30.000,00 a R$ 34.999,99', 4
FROM regras_comissao WHERE nome = 'Regra Padrão';

INSERT INTO faixas_comissao_config (regra_id, min_faturamento, max_faturamento, porcentagem, descricao, ordem)
SELECT id, 35000.00, NULL, 25.00, 'Acima de R$ 35.000,00', 5
FROM regras_comissao WHERE nome = 'Regra Padrão';

-- ===========================================
-- 6. CLIENTE DE TESTE
-- ===========================================
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

-- ===========================================
-- 7. CATÁLOGO DE TESTE
-- ===========================================
INSERT INTO tipos_peca (empresa_id, nome, valor_padrao) VALUES 
((SELECT id FROM empresas WHERE nome = 'Empresa Padrão'), 'Limpeza Simples', 50.00),
((SELECT id FROM empresas WHERE nome = 'Empresa Padrão'), 'Polimento', 150.00),
((SELECT id FROM empresas WHERE nome = 'Empresa Padrão'), 'Higienização Interna', 120.00);
