ALTER TABLE users ADD COLUMN participa_comissao BOOLEAN DEFAULT TRUE;

COMMENT ON COLUMN users.participa_comissao IS 'Indica se o usuário participa do sistema de comissões. Default TRUE.';
