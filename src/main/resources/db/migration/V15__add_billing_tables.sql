-- 1. Faturas de Licença (Dono -> Revendedor)
CREATE TABLE faturas_licenca (
    id BIGSERIAL PRIMARY KEY,
    licenca_id BIGINT NOT NULL,
    mes_referencia VARCHAR(7) NOT NULL, -- '2026-01'
    
    -- Valores Detalhados
    valor_mensalidade DECIMAL(10,2) NOT NULL,
    quantidade_tenants INT NOT NULL,
    valor_por_tenant DECIMAL(10,2) NOT NULL,
    valor_tenants DECIMAL(10,2) NOT NULL,
    valor_total DECIMAL(10,2) NOT NULL,
    
    -- Pagamento
    data_emissao DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    valor_pago DECIMAL(10,2),
    forma_pagamento VARCHAR(50),
    comprovante_url VARCHAR(500),
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    observacoes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_fatura_licenca_licenca FOREIGN KEY (licenca_id) REFERENCES licencas(id)
);

CREATE INDEX idx_faturas_licenca_licenca ON faturas_licenca(licenca_id);
CREATE INDEX idx_faturas_licenca_mes ON faturas_licenca(mes_referencia);
CREATE INDEX idx_faturas_licenca_status ON faturas_licenca(status);


-- 2. Faturas de Tenant (Revendedor -> Empresa)
CREATE TABLE faturas_tenant (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    licenca_id BIGINT NOT NULL, -- Para facilitar queries do revendedor
    mes_referencia VARCHAR(7) NOT NULL,
    
    valor DECIMAL(10,2) NOT NULL,
    
    -- Datas
    data_emissao DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    valor_pago DECIMAL(10,2),
    
    -- Gateway Integration
    gateway_pagamento VARCHAR(50),
    payment_id VARCHAR(255),
    preference_id VARCHAR(255),
    qr_code_pix TEXT,
    qr_code_image_url VARCHAR(500),
    url_pagamento VARCHAR(500),
    tentativas_cobranca INT DEFAULT 0,
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_fatura_tenant_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_fatura_tenant_licenca FOREIGN KEY (licenca_id) REFERENCES licencas(id)
);

CREATE INDEX idx_faturas_tenant_empresa ON faturas_tenant(empresa_id);
CREATE INDEX idx_faturas_tenant_licenca ON faturas_tenant(licenca_id);
CREATE INDEX idx_faturas_tenant_status ON faturas_tenant(status);
CREATE INDEX idx_faturas_tenant_payment_id ON faturas_tenant(payment_id);
