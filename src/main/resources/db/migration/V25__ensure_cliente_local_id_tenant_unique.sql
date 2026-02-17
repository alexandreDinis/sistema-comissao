-- V25__ensure_cliente_local_id_tenant_unique.sql
-- Idempotent migration: ensures localId uniqueness is per-tenant (empresa_id, local_id)
-- instead of global (local_id only). Safe to run on databases where V24 already ran.

-- 1) Drop ALL single-column UNIQUE constraints on (local_id)
DO $$
DECLARE
  r record;
BEGIN
  FOR r IN
    SELECT con.conname AS constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
    WHERE rel.relname = 'clientes'
      AND nsp.nspname = 'public'
      AND con.contype = 'u'
      AND array_length(con.conkey, 1) = 1
      AND con.conkey[1] = (
        SELECT attnum
        FROM pg_attribute
        WHERE attrelid = rel.oid
          AND attname = 'local_id'
          AND NOT attisdropped
      )
  LOOP
    EXECUTE format('ALTER TABLE public.clientes DROP CONSTRAINT %I', r.constraint_name);
    RAISE NOTICE 'Dropped unique constraint: %', r.constraint_name;
  END LOOP;

  IF NOT FOUND THEN
    RAISE NOTICE 'No single-column UNIQUE constraint on local_id found';
  END IF;
END $$;

-- 1b) Drop ALL single-column UNIQUE indexes on (local_id)
--     Covers case where uniqueness was enforced via CREATE UNIQUE INDEX instead of ADD CONSTRAINT
DO $$
DECLARE
  r record;
BEGIN
  FOR r IN
    SELECT i.relname AS index_name
    FROM pg_index idx
    JOIN pg_class t ON t.oid = idx.indrelid
    JOIN pg_namespace nsp ON nsp.oid = t.relnamespace
    JOIN pg_class i ON i.oid = idx.indexrelid
    WHERE t.relname = 'clientes'
      AND nsp.nspname = 'public'
      AND idx.indisunique = true
      AND idx.indisprimary = false
      AND idx.indnatts = 1
      AND (idx.indkey::int[])[1] = (
        SELECT attnum
        FROM pg_attribute
        WHERE attrelid = t.oid
          AND attname = 'local_id'
          AND NOT attisdropped
      )
      -- Exclude the index backing any remaining constraint (already handled above)
      AND NOT EXISTS (
        SELECT 1 FROM pg_constraint con
        WHERE con.conindid = idx.indexrelid
      )
  LOOP
    EXECUTE format('DROP INDEX public.%I', r.index_name);
    RAISE NOTICE 'Dropped unique index: %', r.index_name;
  END LOOP;

  IF NOT FOUND THEN
    RAISE NOTICE 'No orphan single-column UNIQUE index on local_id found';
  END IF;
END $$;

-- 2) Ensure composite unique (empresa_id, local_id) exists — checked by definition, not name
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
    WHERE rel.relname = 'clientes'
      AND nsp.nspname = 'public'
      AND con.contype = 'u'
      AND array_length(con.conkey, 1) = 2
      AND con.conkey @> ARRAY[
        (SELECT attnum FROM pg_attribute WHERE attrelid = rel.oid AND attname = 'empresa_id' AND NOT attisdropped),
        (SELECT attnum FROM pg_attribute WHERE attrelid = rel.oid AND attname = 'local_id'   AND NOT attisdropped)
      ]::smallint[]
  ) THEN
    ALTER TABLE public.clientes
      ADD CONSTRAINT uk_clientes_empresa_local_id UNIQUE (empresa_id, local_id);
    RAISE NOTICE 'Created composite unique constraint uk_clientes_empresa_local_id';
  ELSE
    RAISE NOTICE 'Composite UNIQUE(empresa_id, local_id) already exists';
  END IF;
END $$;

-- 3) Performance indexes for delta sync queries
CREATE INDEX IF NOT EXISTS idx_clientes_empresa_updated_at
  ON public.clientes (empresa_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_clientes_empresa_deleted_at
  ON public.clientes (empresa_id, deleted_at);
