-- Fix foreign key: role.location_id should reference base.branch(location_id)
-- Safe drop (if previous FK exists) and recreate with correct target

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE c.conname = 'fk_role_location'
          AND n.nspname = 'base'
          AND t.relname = 'role'
    ) THEN
        EXECUTE 'ALTER TABLE base.role DROP CONSTRAINT fk_role_location';
    END IF;
END $$;

-- Optional: ensure no orphan rows before adding FK (will fail if any)
-- SELECT r.location_id FROM base.role r LEFT JOIN base.branch b ON b.location_id = r.location_id WHERE b.location_id IS NULL;

ALTER TABLE base.role
ADD CONSTRAINT fk_role_branch
FOREIGN KEY (location_id)
REFERENCES base.branch(location_id);
