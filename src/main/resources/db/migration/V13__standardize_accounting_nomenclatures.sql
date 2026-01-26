-- Migration to standardize Accounting Nomenclatures (DRE & Cash Flow)

-- 1. Update TipoContaPagar (Refactoring)
UPDATE contas_pagar SET tipo = 'OPERACIONAL' WHERE tipo = 'DESPESA_OPERACIONAL';
UPDATE contas_pagar SET tipo = 'FOLHA_PAGAMENTO' WHERE tipo IN ('COMISSAO_FUNCIONARIO', 'ADIANTAMENTO', 'SALARIO');
UPDATE contas_pagar SET tipo = 'OPERACIONAL' WHERE tipo = 'FORNECEDOR';
UPDATE contas_pagar SET tipo = 'IMPOSTOS' WHERE tipo IN ('IMPOSTO', 'IMPOSTO_PAGO');

-- 2. Update CategoriaDespesa (Refactoring)
-- Mapping old functional categories to DRE Groups

-- Let's update removed/renamed categories
UPDATE despesas SET categoria = 'BENEFICIOS' WHERE categoria = 'ALIMENTACAO';
UPDATE despesas SET categoria = 'OCUPACAO' WHERE categoria = 'ALUGUEL';
UPDATE despesas SET categoria = 'UTILIDADES' WHERE categoria IN ('ENERGIA_AGUA', 'INTERNET_TELEFONE');
UPDATE despesas SET categoria = 'MANUTENCAO_PREDIAL' WHERE categoria = 'MANUTENCAO';
UPDATE despesas SET categoria = 'MATERIAIS_APLICADOS' WHERE categoria = 'FERRAMENTAS';
UPDATE despesas SET categoria = 'MATERIAL_USO_CONSUMO' WHERE categoria = 'MATERIAL_ESCRITORIO';
UPDATE despesas SET categoria = 'SERVICOS_TERCEIROS' WHERE categoria = 'SERVICOS_TERCEIROS';
UPDATE despesas SET categoria = 'TARIFAS_BANCARIAS' WHERE categoria = 'TAXAS_BANCARIAS';
UPDATE despesas SET categoria = 'SERVICOS_PROFISSIONAIS' WHERE categoria = 'CONTABILIDADE';
