CREATE TABLE amenities (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL UNIQUE,
    icono VARCHAR(255)
);

CREATE TABLE workspace_amenities (
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (workspace_id, amenity_id)
);

INSERT INTO amenities (nombre, icono) VALUES
    ('WiFi', 'wifi'),
    ('Café', 'coffee'),
    ('Silla Ergonómica', 'chair');
