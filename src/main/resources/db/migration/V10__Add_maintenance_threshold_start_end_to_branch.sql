-- Add start and end maintenance threshold times to branch table
ALTER TABLE base.branch
    ADD COLUMN maintenance_threshold_start_time TIME,
    ADD COLUMN maintenance_threshold_end_time TIME;
