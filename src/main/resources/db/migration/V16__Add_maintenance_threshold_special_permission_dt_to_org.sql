-- Add maintenance_threshold_special_permission_dt column to base.org
ALTER TABLE base.org
    ADD COLUMN maintenance_threshold_special_permission_dt INT;
