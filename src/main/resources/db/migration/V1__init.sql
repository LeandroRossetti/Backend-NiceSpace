CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE team_members (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ,
    CONSTRAINT uq_team_member UNIQUE (team_id, user_id)
);

CREATE TABLE team_invitations (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE workspaces (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    map_structure JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id),
    nombre VARCHAR(255) NOT NULL,
    capacidad INTEGER NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resources(id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    team_id BIGINT REFERENCES teams(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reservations_resource_time ON reservations (resource_id, start_time, end_time);
CREATE INDEX idx_reservations_created_by ON reservations (created_by);
CREATE INDEX idx_reservations_team ON reservations (team_id);

-- RT-02: prevencion de overbooking a nivel de base de datos.
ALTER TABLE reservations ADD CONSTRAINT no_overlap
EXCLUDE USING gist (resource_id WITH =, tstzrange(start_time, end_time) WITH &&)
WHERE (status <> 'CANCELADA');
