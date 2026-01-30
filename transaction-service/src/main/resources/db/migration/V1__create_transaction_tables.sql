-- Crear tabla de tipos de transacción
CREATE TABLE transaction_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Crear tabla de estados de transacción
CREATE TABLE transaction_statuses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Crear tabla de transacciones
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_external_id UUID NOT NULL UNIQUE,
    account_external_id_debit UUID NOT NULL,
    account_external_id_credit UUID NOT NULL,
    transfer_type_id INTEGER NOT NULL REFERENCES transaction_types(id),
    status_id INTEGER NOT NULL REFERENCES transaction_statuses(id),
    value NUMERIC(19, 2) NOT NULL CHECK (value > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Crear índices para mejor rendimiento de consultas
CREATE INDEX idx_transaction_external_id ON transactions(transaction_external_id);
CREATE INDEX idx_transaction_status ON transactions(status_id);
CREATE INDEX idx_created_at ON transactions(created_at DESC);
CREATE INDEX idx_account_debit ON transactions(account_external_id_debit);
CREATE INDEX idx_account_credit ON transactions(account_external_id_credit);

-- Crear índice compuesto para consultas comunes
CREATE INDEX idx_status_created ON transactions(status_id, created_at DESC);

-- Insertar tipos de transacción iniciales
INSERT INTO transaction_types (name, description) VALUES
    ('transfer', 'Transferencia regular entre cuentas'),
    ('payment', 'Transacción de pago'),
    ('refund', 'Transacción de reembolso');

-- Insertar estados de transacción iniciales
INSERT INTO transaction_statuses (name, description) VALUES
    ('pending', 'Transacción pendiente de validación'),
    ('approved', 'Transacción aprobada por antifraude'),
    ('rejected', 'Transacción rechazada por antifraude');
