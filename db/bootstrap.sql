DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'familyapp') THEN
        CREATE ROLE familyapp LOGIN PASSWORD 'familyapp';
    END IF;
END
$$;

CREATE DATABASE familyapp OWNER familyapp;
