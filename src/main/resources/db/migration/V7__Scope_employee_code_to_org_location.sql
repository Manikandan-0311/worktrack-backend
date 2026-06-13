-- Update employee code uniqueness constraint to be scoped by both org_id and location_id
-- This allows the same employee code to be used in different organizations or locations

-- Drop the existing unique constraint on (org_id, employee_code)
ALTER TABLE base.employee DROP CONSTRAINT IF EXISTS uk_org_employee_code;

-- Add new unique constraint on (org_id, location_id, employee_code)
ALTER TABLE base.employee ADD CONSTRAINT uk_org_location_employee_code UNIQUE (org_id, location_id, employee_code);
