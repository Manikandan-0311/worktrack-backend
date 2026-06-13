-- Add nullable reporting_to column to base.employee (no FK constraint)
ALTER TABLE base.employee
    ADD COLUMN reporting_to integer;

-- Optionally create an index for faster lookups on reporting_to
CREATE INDEX IF NOT EXISTS idx_employee_reporting_to ON base.employee(reporting_to);
