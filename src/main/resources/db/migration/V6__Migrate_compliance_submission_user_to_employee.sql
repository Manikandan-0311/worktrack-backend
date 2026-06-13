-- Migrate compliance.compliance_submission FK from base.user(user_id) to base.employee(employee_id)
BEGIN;

-- 1) Add new column if missing
ALTER TABLE compliance.compliance_submission
    ADD COLUMN IF NOT EXISTS employee_id INTEGER;

-- 2) Backfill from legacy user_id if it exists
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'compliance' AND table_name = 'compliance_submission' AND column_name = 'user_id'
  ) THEN
    UPDATE compliance.compliance_submission s
    SET employee_id = u.employee_id
    FROM base."user" u
    WHERE u.user_id = s.user_id
      AND s.employee_id IS NULL;

    -- Drop legacy column
    ALTER TABLE compliance.compliance_submission DROP COLUMN user_id;
  END IF;
END $$;

-- 2b) Fallback backfill using created_by if it references base.user
UPDATE compliance.compliance_submission s
SET employee_id = u.employee_id
FROM base."user" u
WHERE s.employee_id IS NULL AND s.created_by = u.user_id;

-- 3) Add FK (allows NULLs) and index
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'fk_compliance_submission_employee'
      AND conrelid = 'compliance.compliance_submission'::regclass
  ) THEN
    ALTER TABLE compliance.compliance_submission
      ADD CONSTRAINT fk_compliance_submission_employee
      FOREIGN KEY (employee_id) REFERENCES base.employee(employee_id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_compliance_submission_employee_id
  ON compliance.compliance_submission (employee_id);

-- 4) Set NOT NULL only if there are no NULLs remaining
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM compliance.compliance_submission WHERE employee_id IS NULL) THEN
    ALTER TABLE compliance.compliance_submission ALTER COLUMN employee_id SET NOT NULL;
  END IF;
END $$;

COMMIT;
