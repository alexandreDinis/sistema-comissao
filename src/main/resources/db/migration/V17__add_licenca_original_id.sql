-- V17: Add licenca_original_id for reseller history tracking
-- When a reseller rescinds, we set licenca_id = NULL but keep licenca_original_id
-- to track who originally brought the tenant

ALTER TABLE empresas ADD COLUMN licenca_original_id BIGINT;

ALTER TABLE empresas 
    ADD CONSTRAINT fk_empresas_licenca_original 
    FOREIGN KEY (licenca_original_id) REFERENCES licencas(id);

-- Populate original with current for existing relationships
UPDATE empresas SET licenca_original_id = licenca_id WHERE licenca_id IS NOT NULL;
