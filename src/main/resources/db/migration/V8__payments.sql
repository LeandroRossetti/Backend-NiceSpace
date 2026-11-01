-- Pagos de reservas con Mercado Pago (Checkout Pro, sandbox).
-- Una reserva tiene a lo sumo un pago (1:1). El estado del pago es independiente
-- del estado de la reserva (CONFIRMADA/CANCELADA/COMPLETADA).
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE REFERENCES reservations(id),
    preference_id VARCHAR(255) NOT NULL,
    payment_id VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    init_point TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
