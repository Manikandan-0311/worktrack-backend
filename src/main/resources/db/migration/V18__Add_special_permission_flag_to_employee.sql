-- Add special_permission_flag column to employee table
ALTER TABLE base.employee
ADD COLUMN special_permission_flag BOOLEAN DEFAULT FALSE;
