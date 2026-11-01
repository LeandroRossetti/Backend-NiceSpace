-- Migración a la API de Orders de Mercado Pago.
-- La "preferencia" del flujo clásico pasa a ser una "order" y el link de pago
-- deja de ser "init_point" para llamarse "checkout_url".
ALTER TABLE payments RENAME COLUMN preference_id TO order_id;
ALTER TABLE payments RENAME COLUMN init_point TO checkout_url;
