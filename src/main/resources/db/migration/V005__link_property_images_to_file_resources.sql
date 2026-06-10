ALTER TABLE property_images
    ADD COLUMN file_resource_id BIGINT;

ALTER TABLE property_images
    ADD CONSTRAINT uk_property_images_file_resource UNIQUE (file_resource_id);

ALTER TABLE property_images
    ADD CONSTRAINT fk_property_images_file_resource
        FOREIGN KEY (file_resource_id) REFERENCES file_resources (id);
