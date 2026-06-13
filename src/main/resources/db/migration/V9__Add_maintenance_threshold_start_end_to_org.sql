-- Add start and end maintenance threshold times to org table
ALTER TABLE base.org
    ADD COLUMN maintenance_threshold_start_time TIME,
    ADD COLUMN maintenance_threshold_end_time TIME;
