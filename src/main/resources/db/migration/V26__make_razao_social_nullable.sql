-- Make razao_social nullable — only nome_fantasia is now required for client creation
ALTER TABLE public.clientes ALTER COLUMN razao_social DROP NOT NULL;
