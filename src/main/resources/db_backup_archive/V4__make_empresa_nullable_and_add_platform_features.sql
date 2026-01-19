-- V4__make_empresa_nullable_and_add_platform_features.sql
-- Refatoração: Desacoplar Super Admin de Empresa e adicionar Features de Plataforma

-- 1. Permitir que empresa_id seja nulo na tabela usuarios
ALTER TABLE users ALTER COLUMN empresa_id DROP NOT NULL;

-- 2. Atualizar o Super Admin para não ter empresa
UPDATE users 
SET empresa_id = NULL 
WHERE email = 'saas@plataforma.com';

-- 3. Remover a 'SaaS Platform' company (criada na V3)
DELETE FROM empresas WHERE nome = 'SaaS Platform';

-- 4. Criar Features de Plataforma
INSERT INTO features (codigo, descricao, plano_minimo) VALUES 
('PLATFORM_DASHBOARD_VIEW', 'Visualizar Dashboard da Plataforma', 'OURO'),
('PLATFORM_COMPANY_MANAGE', 'Gerenciar Empresas (SaaS)', 'OURO'),
('PLATFORM_PLAN_MANAGE', 'Gerenciar Planos (SaaS)', 'OURO')
ON CONFLICT (codigo) DO NOTHING;

-- 5. Dar permissões de Plataforma para o Super Admin
INSERT INTO user_features (usuario_id, feature_id)
SELECT u.id, f.id
FROM users u, features f
WHERE u.email = 'saas@plataforma.com'
AND f.codigo IN ('PLATFORM_DASHBOARD_VIEW', 'PLATFORM_COMPANY_MANAGE', 'PLATFORM_PLAN_MANAGE')
ON CONFLICT DO NOTHING;
