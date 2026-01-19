-- Migration V7: Add branding and contact fields to Empresas
-- Adds support for professional PDF generation with company details

ALTER TABLE empresas
ADD COLUMN razao_social VARCHAR(255),
ADD COLUMN logo_path VARCHAR(255),
ADD COLUMN endereco VARCHAR(255),
ADD COLUMN telefone VARCHAR(50),
ADD COLUMN email VARCHAR(100);

-- Initialize razao_social with existing nome (essential for not null constraints if we add them later)
UPDATE empresas SET razao_social = nome WHERE razao_social IS NULL;

-- Comment on columns
COMMENT ON COLUMN empresas.razao_social IS 'Razão Social para documentos oficiais';
COMMENT ON COLUMN empresas.logo_path IS 'Caminho do arquivo de logo no servidor';
COMMENT ON COLUMN empresas.endereco IS 'Endereço completo da empresa';
COMMENT ON COLUMN empresas.telefone IS 'Telefone de contato';
COMMENT ON COLUMN empresas.email IS 'Email de contato';
