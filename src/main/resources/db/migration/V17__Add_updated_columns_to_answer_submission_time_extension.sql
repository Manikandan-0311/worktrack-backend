-- Add updated_by and updated_dt to compliance.answer_submission_time_extension
ALTER TABLE compliance.answer_submission_time_extension
    ADD COLUMN updated_by INT,
    ADD COLUMN updated_dt TIMESTAMP;
