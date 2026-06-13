-- Migrate auth.auth_token FK from base.user(user_id) to base.employee(employee_id)
-- Assumes PostgreSQL. Wrap in a transaction for safety.

BEGIN;

-- 1) Add the new column (nullable for backfill)
ALTER TABLE auth.auth_token
    ADD COLUMN IF NOT EXISTS employee_id INTEGER;

-- 2) Backfill only if legacy column user_id still exists
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'auth' AND table_name = 'auth_token' AND column_name = 'user_id'
  ) THEN
    UPDATE auth.auth_token t
    SET employee_id = u.employee_id
    FROM base."user" u
    WHERE u.user_id = t.user_id
      AND t.employee_id IS NULL;

    -- Drop legacy column after backfill
    ALTER TABLE auth.auth_token DROP COLUMN user_id;
  END IF;
END $$;

-- 3) Guard against NULL employee_id rows
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM auth.auth_token WHERE employee_id IS NULL) THEN
    RAISE EXCEPTION 'Migration aborted: auth.auth_token has NULL employee_id rows after backfill';
  END IF;
END $$;

-- 4) Set NOT NULL on the new column
ALTER TABLE auth.auth_token
    ALTER COLUMN employee_id SET NOT NULL;

-- 5) Add the new FK constraint to base.employee if not exists
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'fk_auth_token_employee'
      AND conrelid = 'auth.auth_token'::regclass
  ) THEN
    ALTER TABLE auth.auth_token
      ADD CONSTRAINT fk_auth_token_employee
      FOREIGN KEY (employee_id) REFERENCES base.employee(employee_id);
  END IF;
END $$;

-- 6) Drop any old FK constraints referring to user_id (names may vary)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'fk_auth_token_user'
      AND conrelid = 'auth.auth_token'::regclass
  ) THEN
    ALTER TABLE auth.auth_token DROP CONSTRAINT fk_auth_token_user;
  END IF;
  IF EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'auth_token_user_id_fkey'
      AND conrelid = 'auth.auth_token'::regclass
  ) THEN
    ALTER TABLE auth.auth_token DROP CONSTRAINT auth_token_user_id_fkey;
  END IF;
END $$;

-- 7) Helpful index on employee_id for lookups
CREATE INDEX IF NOT EXISTS idx_auth_token_employee_id
    ON auth.auth_token (employee_id);

COMMIT;
