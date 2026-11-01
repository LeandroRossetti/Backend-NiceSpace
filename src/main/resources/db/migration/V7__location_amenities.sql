-- Mueve los amenities del espacio (workspace) a la sede (location).
-- Se crea la tabla N:M location_amenities y se elimina workspace_amenities.
CREATE TABLE location_amenities (
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (location_id, amenity_id)
);

DROP TABLE workspace_amenities;
