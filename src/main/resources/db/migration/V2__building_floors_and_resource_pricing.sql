CREATE TABLE floors (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    level_order INTEGER NOT NULL,
    map_structure JSONB,
    CONSTRAINT uq_floor_workspace_order UNIQUE (workspace_id, level_order)
);

INSERT INTO floors (workspace_id, nombre, level_order, map_structure)
SELECT id, 'Piso 1', 1, map_structure
FROM workspaces;

ALTER TABLE resources ADD COLUMN floor_id BIGINT;
ALTER TABLE resources ADD COLUMN hourly_price NUMERIC(12, 2);
ALTER TABLE resources ADD COLUMN daily_price NUMERIC(12, 2);
ALTER TABLE resources ADD COLUMN monthly_price NUMERIC(12, 2);
ALTER TABLE resources ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'ARS';

UPDATE resources r
SET floor_id = f.id
FROM floors f
WHERE f.workspace_id = r.workspace_id;

ALTER TABLE resources ALTER COLUMN floor_id SET NOT NULL;
ALTER TABLE resources ADD CONSTRAINT fk_resources_floor FOREIGN KEY (floor_id) REFERENCES floors(id);
ALTER TABLE resources ADD CONSTRAINT chk_resources_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE resources ADD CONSTRAINT chk_resources_hourly_price CHECK (hourly_price IS NULL OR hourly_price >= 0);
ALTER TABLE resources ADD CONSTRAINT chk_resources_daily_price CHECK (daily_price IS NULL OR daily_price >= 0);
ALTER TABLE resources ADD CONSTRAINT chk_resources_monthly_price CHECK (monthly_price IS NULL OR monthly_price >= 0);
