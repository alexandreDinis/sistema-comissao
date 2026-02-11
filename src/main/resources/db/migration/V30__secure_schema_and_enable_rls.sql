DO $$
DECLARE
  r RECORD;
  has_anon BOOLEAN;
  has_authenticated BOOLEAN;
  has_postgrest BOOLEAN;
  has_service_role BOOLEAN;
  has_backend_role BOOLEAN;
  has_postgres BOOLEAN;
  backend_role_name TEXT := '${backend_role}';
BEGIN
  -- 0) Environment Check
  IF '${apply_rls}' <> 'true' THEN
    RAISE NOTICE 'Skipping RLS enforcement for non-production environment';
    RETURN;
  END IF;

  -- 0.1) Fail-Fast Empty Check
  IF backend_role_name IS NULL OR backend_role_name = '' THEN
    RAISE EXCEPTION 'backend_role placeholder is empty. Refusing to apply security policies.';
  END IF;

  -- 1) Detect Roles
  SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='anon') INTO has_anon;
  SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='authenticated') INTO has_authenticated;
  SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='postgrest') INTO has_postgrest;
  SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='service_role') INTO has_service_role;
  SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='postgres') INTO has_postgres;
  SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname=backend_role_name) INTO has_backend_role;

  -- 2) Fail-Fast Existence Verification
  IF NOT has_backend_role THEN
    RAISE EXCEPTION 'Backend role "%" not found. Cannot safely apply security policies.', backend_role_name;
  END IF;

  -- 3) Revoke Grants (Current Tables - including PUBLIC)
  EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC';
  IF has_anon THEN EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon'; END IF;
  IF has_authenticated THEN EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA public FROM authenticated'; END IF;
  IF has_postgrest THEN EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA public FROM postgrest'; END IF;
  
  -- 4) Revoke Grants (Future Tables via Default Privileges - including PUBLIC)
  -- Apply for 'postgres' (common creator)
  IF has_postgres THEN
    EXECUTE 'ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES FROM PUBLIC';
    IF has_anon THEN EXECUTE 'ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES FROM anon'; END IF;
    IF has_authenticated THEN EXECUTE 'ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES FROM authenticated'; END IF;
    IF has_postgrest THEN EXECUTE 'ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES FROM postgrest'; END IF;
  END IF;
  
  -- Apply for current user (Flyway user)
  EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM PUBLIC';
  IF has_anon THEN EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon'; END IF;
  IF has_authenticated THEN EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM authenticated'; END IF;
  IF has_postgrest THEN EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM postgrest'; END IF;

  -- 5) Loop Tables & Apply RLS
  FOR r IN
    SELECT c.relname AS tablename
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relkind IN ('r', 'p') -- Ordinary tables + Partitioned tables
      AND c.relname <> 'flyway_schema_history'
  LOOP
    -- Enable & Force RLS
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY;', r.tablename);
    EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY;', r.tablename);

    -- Clean old policy
    EXECUTE format('DROP POLICY IF EXISTS allow_backend_full_access ON public.%I;', r.tablename);

    -- Create New Policy (Strict)
    IF has_service_role THEN
       EXECUTE format('CREATE POLICY allow_backend_full_access ON public.%I FOR ALL TO %I, service_role USING (true) WITH CHECK (true);', r.tablename, backend_role_name);
    ELSE
       EXECUTE format('CREATE POLICY allow_backend_full_access ON public.%I FOR ALL TO %I USING (true) WITH CHECK (true);', r.tablename, backend_role_name);
    END IF;

  END LOOP;
  
  RAISE NOTICE 'RLS Forced, Defaults Secured, and Backend Policy Applied for %', backend_role_name;
END $$;
