-- Adiciona dia de fechamento para cartões de crédito
ALTER TABLE cartoes_credito ADD COLUMN dia_fechamento INTEGER NOT NULL DEFAULT 25;

-- Comentário: Considera que despesas após o dia de fechamento vão para a fatura do mês seguinte
