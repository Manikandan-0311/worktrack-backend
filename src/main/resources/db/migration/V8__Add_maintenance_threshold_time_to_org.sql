-- Add maintenance_threshold_time (time only) to base.org
ALTER TABLE base.org
ADD COLUMN IF NOT EXISTS maintenance_threshold_time TIME;
