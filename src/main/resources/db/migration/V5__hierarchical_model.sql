-- Modelo jerárquico: Location (sede/edificio) -> Workspace (espacio alquilable)
-- Se reutilizan las tablas existentes por renombrado:
--   workspaces (edificio) -> locations
--   resources  (espacio) -> workspaces

-- 1) Edificio: workspaces -> locations
ALTER TABLE workspaces RENAME TO locations;

ALTER TABLE locations
    ADD COLUMN address VARCHAR(255),
    ADD COLUMN city VARCHAR(255),
    ADD COLUMN province VARCHAR(255),
    ADD COLUMN zip_code VARCHAR(20),
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION,
    ADD COLUMN description TEXT,
    ADD COLUMN contact_email VARCHAR(255),
    ADD COLUMN contact_phone VARCHAR(50),
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 2) Espacio alquilable: resources -> workspaces
ALTER TABLE resources RENAME TO workspaces;

ALTER TABLE workspaces RENAME COLUMN workspace_id TO location_id;
ALTER TABLE workspaces RENAME COLUMN capacidad TO capacity;
ALTER TABLE workspaces RENAME COLUMN tipo TO workspace_type;
ALTER TABLE workspaces RENAME COLUMN active TO is_active;
ALTER TABLE workspaces RENAME COLUMN hourly_price TO price_per_hour;
ALTER TABLE workspaces RENAME COLUMN daily_price TO price_per_day;
ALTER TABLE workspaces RENAME COLUMN monthly_price TO price_per_month;

-- Un espacio puede crearse antes de ser ubicado en un piso del plano
ALTER TABLE workspaces ALTER COLUMN floor_id DROP NOT NULL;

ALTER TABLE workspaces
    ADD COLUMN max_hours INTEGER NOT NULL DEFAULT 24,
    ADD COLUMN max_days INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN max_months INTEGER NOT NULL DEFAULT 12,
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 3) Pisos: workspace_id -> location_id
ALTER TABLE floors RENAME COLUMN workspace_id TO location_id;

-- 4) Reservas: resource_id -> workspace_id + modalidad + precio total
ALTER TABLE reservations RENAME COLUMN resource_id TO workspace_id;
ALTER TABLE reservations
    ADD COLUMN rental_type VARCHAR(20) NOT NULL DEFAULT 'HORA',
    ADD COLUMN total_price NUMERIC(12, 2);

ALTER TABLE reservations DROP CONSTRAINT no_overlap;
DROP INDEX idx_reservations_resource_time;
CREATE INDEX idx_reservations_workspace_time ON reservations (workspace_id, start_time, end_time);
ALTER TABLE reservations ADD CONSTRAINT no_overlap
    EXCLUDE USING gist (workspace_id WITH =, tstzrange(start_time, end_time) WITH &&)
    WHERE (status <> 'CANCELADA');

-- 5) Amenities cuelgan del espacio (workspace), no del edificio
DROP TABLE workspace_amenities;
CREATE TABLE workspace_amenities (
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    amenity_id   BIGINT NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (workspace_id, amenity_id)
);
