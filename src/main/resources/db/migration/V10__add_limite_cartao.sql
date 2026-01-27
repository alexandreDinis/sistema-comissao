-- Adiciona coluna de limite no cartão de crédito
ALTER TABLE cartoes_credito ADD COLUMN limite DECIMAL(19,2);
