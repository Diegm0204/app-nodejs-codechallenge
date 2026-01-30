-- Agregar columna idempotency_key para prevenir transacciones duplicadas
ALTER TABLE transactions
ADD COLUMN idempotency_key VARCHAR(255);

-- Crear índice único para garantizar que no haya duplicados
-- NOTA: Permitimos NULL porque transacciones existentes no tienen este campo
CREATE UNIQUE INDEX idx_transactions_idempotency_key 
ON transactions(idempotency_key) 
WHERE idempotency_key IS NOT NULL;

-- Comentario de documentación
COMMENT ON COLUMN transactions.idempotency_key IS 
'UUID generado por el cliente para prevenir transacciones duplicadas por reintentos o doble clic';
