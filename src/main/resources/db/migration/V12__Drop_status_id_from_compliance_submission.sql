-- Drop the status_id column from compliance_submission (if present)
-- Uses CASCADE to remove dependent foreign key constraints if any
ALTER TABLE compliance.compliance_submission
    DROP COLUMN IF EXISTS status_id CASCADE;
