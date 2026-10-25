-- Google Sign-In: users authenticated with Google have no local password.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Stable Google account identifier ("sub" claim). Unique per Google account.
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT uq_users_google_sub UNIQUE (google_sub);
