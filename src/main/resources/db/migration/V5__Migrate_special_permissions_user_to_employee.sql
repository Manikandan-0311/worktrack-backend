-- Migrate special_permissions from user_id to employee_id and adjust granted_by to reference employee

-- 1) Add employee_id column if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'compliance' AND table_name = 'special_permissions' AND column_name = 'employee_id'
    ) THEN
        ALTER TABLE compliance.special_permissions ADD COLUMN employee_id INT;
    END IF;
END$$;

-- 2) Backfill employee_id from base."user" mapping
UPDATE compliance.special_permissions s
SET employee_id = u.employee_id
FROM base."user" u
WHERE s.employee_id IS NULL AND s.user_id = u.user_id;

-- 3) Set NOT NULL on employee_id
ALTER TABLE compliance.special_permissions ALTER COLUMN employee_id SET NOT NULL;

-- 4) Add FK and index for employee_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_schema = 'compliance' AND table_name = 'special_permissions' AND constraint_name = 'fk_special_permissions_employee'
    ) THEN
        ALTER TABLE compliance.special_permissions 
            ADD CONSTRAINT fk_special_permissions_employee FOREIGN KEY (employee_id) REFERENCES base.employee(employee_id);
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_special_permissions_employee ON compliance.special_permissions(employee_id);

-- 5) Update granted_by to store employee_id instead of user_id and add FK
UPDATE compliance.special_permissions s
SET granted_by = u.employee_id
FROM base."user" u
WHERE s.granted_by IS NOT NULL AND s.granted_by = u.user_id;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_schema = 'compliance' AND table_name = 'special_permissions' AND constraint_name = 'fk_special_permissions_granted_by_employee'
    ) THEN
        ALTER TABLE compliance.special_permissions 
            ADD CONSTRAINT fk_special_permissions_granted_by_employee FOREIGN KEY (granted_by) REFERENCES base.employee(employee_id);
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_special_permissions_granted_by ON compliance.special_permissions(granted_by);

-- 6) Drop legacy user_id column if present (old unique constraint will be removed implicitly)
ALTER TABLE compliance.special_permissions DROP COLUMN IF EXISTS user_id;

-- 7) Add new unique constraint for (employee_id, permission_date, permission_type)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_schema = 'compliance' AND table_name = 'special_permissions' AND constraint_name = 'uq_special_permissions_employee_date_type'
    ) THEN
        ALTER TABLE compliance.special_permissions 
            ADD CONSTRAINT uq_special_permissions_employee_date_type UNIQUE (employee_id, permission_date, permission_type);
    END IF;
END$$;
