-- Add foreign key for location_id in Role table
ALTER TABLE role
ADD COLUMN location_id INT NULL;

ALTER TABLE role
ADD CONSTRAINT fk_role_location
FOREIGN KEY (location_id)
REFERENCES location(location_id);