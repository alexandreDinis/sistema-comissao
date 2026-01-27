-- V3__create_super_admin.sql
-- Cria a empresa "SaaS Platform" e o usuário Super Admin

-- 1. Criar Empresa SaaS (Tenant da Plataforma)
INSERT INTO empresas (nome, cnpj, plano) 
VALUES ('SaaS Platform', '00000000000000', 'OURO');

-- 2. Inserir Usuário Super Admin
-- Senha placeholder (será atualizada pelo AdminSeeder)
INSERT INTO users (empresa_id, email, password, role, active) 
VALUES (
    (SELECT id FROM empresas WHERE nome = 'SaaS Platform'), 
    'saas@plataforma.com', 
    '$2a$10$placeholderHASH...........................', 
    'SUPER_ADMIN',
    true
);

-- 3. Dar Permissões (Opcional, pois SUPER_ADMIN já tem acesso root via código)
-- Mas para consistência, vamos dar acesso a features administrativas
INSERT INTO user_features (usuario_id, feature_id)
SELECT u.id, f.id
FROM users u, features f
WHERE u.email = 'saas@plataforma.com'
AND f.codigo IN ('ADMIN_USERS_WRITE', 'ADMIN_CONFIG');
