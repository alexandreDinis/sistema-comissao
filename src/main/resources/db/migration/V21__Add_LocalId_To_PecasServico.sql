-- Add local_id column to pecas_servico table for offline sync
ALTER TABLE pecas_servico
ADD COLUMN local_id VARCHAR(255);

-- Make it non-nullable after populating (optional, but good practice if we could fill it)
-- For now, just add it. The entity uses UUID by default if null on insert.

-- Add index for performance (since we query by localId + empresa)
CREATE INDEX idx_pecas_servico_local_id ON pecas_servico(local_id);
