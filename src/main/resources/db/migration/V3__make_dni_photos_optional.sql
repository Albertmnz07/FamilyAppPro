-- DNI photos are no longer collected at registration (TN-20). The photo keys
-- were already nullable; their content types were not.
ALTER TABLE persona ALTER COLUMN front_photo_content_type DROP NOT NULL;
ALTER TABLE persona ALTER COLUMN back_photo_content_type DROP NOT NULL;
