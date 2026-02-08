-- Safely drop the existing unique constraint on local_id (which might have a random name)
DO $$
DECLARE
  c_name text;
BEGIN
  SELECT con.conname INTO c_name
  FROM pg_constraint con
  JOIN pg_class rel ON rel.oid = con.conrelid
  JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
  WHERE rel.relname = 'clientes'
    AND nsp.nspname = 'public'
    AND con.contype = 'u'
    -- Safely target the single-column unique constraint on local_id
    AND pg_get_constraintdef(con.oid) ILIKE 'UNIQUE (local_id)'
  LIMIT 1;

  IF c_name IS NOT NULL THEN
    EXECUTE format('ALTER TABLE public.clientes DROP CONSTRAINT %I', c_name);
  END IF;
END $$;

-- Add the new multi-tenant unique constraint
ALTER TABLE public.clientes
  ADD CONSTRAINT uk_clientes_empresa_local_id UNIQUE (empresa_id, local_id);

-- Add composite indices for high-performance delta sync
CREATE INDEX IF NOT EXISTS idx_clientes_empresa_updated_at 
  ON public.clientes(empresa_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_clientes_empresa_deleted_at 
  ON public.clientes(empresa_id, deleted_at);
