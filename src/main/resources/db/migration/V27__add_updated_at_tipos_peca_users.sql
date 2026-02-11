-- V27: Add updated_at to tipos_peca and users for sync tracking

ALTER TABLE tipos_peca ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
UPDATE tipos_peca SET updated_at = NOW() WHERE updated_at IS NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
UPDATE users SET updated_at = NOW() WHERE updated_at IS NULL;
