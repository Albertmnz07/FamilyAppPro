-- Photos moved to object storage (TN-08); V1 still carries the legacy bytea columns.
-- Databases created before Flyway was adopted keep those columns until this migration runs.
ALTER TABLE persona DROP COLUMN front_photo;
ALTER TABLE persona DROP COLUMN back_photo;
