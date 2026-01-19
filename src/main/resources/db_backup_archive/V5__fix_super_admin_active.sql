-- V5__fix_super_admin_active.sql
-- Forçar status ativo para o Super Admin

UPDATE users 
SET active = true 
WHERE email = 'saas@plataforma.com';
