ALTER TABLE users
    ADD COLUMN avatar_file_resource_id BIGINT;

ALTER TABLE users
    ADD CONSTRAINT fk_users_avatar_file_resource
        FOREIGN KEY (avatar_file_resource_id) REFERENCES file_resources (id)
        ON DELETE SET NULL;
