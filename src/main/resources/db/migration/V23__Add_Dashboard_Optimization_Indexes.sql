-- Indice para Faturamentos (YoY e Dash)
CREATE INDEX IF NOT EXISTS idx_faturamentos_empresa_data ON faturamentos(empresa_id, data_faturamento);

-- Indice para Contas a Pagar (Dash)
CREATE INDEX IF NOT EXISTS idx_contas_pagar_empresa_status_venc ON contas_pagar(empresa_id, status, data_vencimento);

-- Indice para Contas a Receber (Dash)
CREATE INDEX IF NOT EXISTS idx_contas_receber_empresa_status_venc ON contas_receber(empresa_id, status, data_vencimento);
