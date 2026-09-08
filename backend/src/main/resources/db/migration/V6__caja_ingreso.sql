-- Separa el ingreso de efectivo del total de salidas.
-- total_cash_out pasa a ser solo movimientos negativos; total_cash_in, los positivos.

ALTER TABLE cash_session
    ADD COLUMN total_cash_in DECIMAL(12, 2) NULL AFTER total_cash_out;

-- Sesiones ya cerradas: rehacer los congelados desde los movimientos.
UPDATE cash_session s
SET
    total_cash_out = (
        SELECT COALESCE(SUM(m.amount), 0)
        FROM cash_movement m
        WHERE m.cash_session_id = s.id
          AND m.amount < 0
    ),
    total_cash_in = (
        SELECT COALESCE(SUM(m.amount), 0)
        FROM cash_movement m
        WHERE m.cash_session_id = s.id
          AND m.amount > 0
    )
WHERE s.closing_amount IS NOT NULL;
