CREATE TABLE customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    user_id BIGINT,
    assigned_agent_id BIGINT,
    created_by BIGINT NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    preferred_contact_method VARCHAR(30),
    notes VARCHAR(2000),
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_code UNIQUE (code),
    CONSTRAINT uk_customers_user UNIQUE (user_id),
    CONSTRAINT fk_customers_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_customers_assigned_agent
        FOREIGN KEY (assigned_agent_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_customers_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_customers_status CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')
    ),
    CONSTRAINT ck_customers_source CHECK (
        source IN ('MANUAL', 'WEBSITE', 'REFERRAL', 'IMPORT', 'OTHER')
    ),
    CONSTRAINT ck_customers_priority CHECK (
        priority IN ('LOW', 'MEDIUM', 'HIGH')
    )
);

CREATE INDEX idx_customers_status_assigned
    ON customers (status, assigned_agent_id);
CREATE INDEX idx_customers_email ON customers (email);
CREATE INDEX idx_customers_phone ON customers (phone);
CREATE INDEX idx_customers_created_by ON customers (created_by);

CREATE TABLE customer_requirements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    property_type_id BIGINT,
    province_id BIGINT,
    district_id BIGINT,
    ward_id BIGINT,
    purpose VARCHAR(30) NOT NULL,
    min_budget DECIMAL(19, 2),
    max_budget DECIMAL(19, 2),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    min_area DECIMAL(12, 2),
    max_area DECIMAL(12, 2),
    min_bedrooms INTEGER,
    min_bathrooms INTEGER,
    description VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_customer_requirements PRIMARY KEY (id),
    CONSTRAINT fk_customer_requirements_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_requirements_property_type
        FOREIGN KEY (property_type_id) REFERENCES property_types (id),
    CONSTRAINT fk_customer_requirements_province
        FOREIGN KEY (province_id) REFERENCES provinces (id),
    CONSTRAINT fk_customer_requirements_district
        FOREIGN KEY (district_id) REFERENCES districts (id),
    CONSTRAINT fk_customer_requirements_ward
        FOREIGN KEY (ward_id) REFERENCES wards (id),
    CONSTRAINT ck_customer_requirements_purpose CHECK (
        purpose IN ('SALE', 'RENT')
    ),
    CONSTRAINT ck_customer_requirements_budget CHECK (
        (min_budget IS NULL OR min_budget >= 0)
        AND (max_budget IS NULL OR max_budget >= 0)
        AND (min_budget IS NULL OR max_budget IS NULL OR min_budget <= max_budget)
    ),
    CONSTRAINT ck_customer_requirements_area CHECK (
        (min_area IS NULL OR min_area >= 0)
        AND (max_area IS NULL OR max_area >= 0)
        AND (min_area IS NULL OR max_area IS NULL OR min_area <= max_area)
    ),
    CONSTRAINT ck_customer_requirements_rooms CHECK (
        (min_bedrooms IS NULL OR min_bedrooms >= 0)
        AND (min_bathrooms IS NULL OR min_bathrooms >= 0)
    )
);

CREATE INDEX idx_customer_requirements_customer_active
    ON customer_requirements (customer_id, active);
CREATE INDEX idx_customer_requirements_location
    ON customer_requirements (province_id, district_id, ward_id);
CREATE INDEX idx_customer_requirements_purpose_budget
    ON customer_requirements (purpose, min_budget, max_budget);

CREATE TABLE customer_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_customer_tags PRIMARY KEY (id),
    CONSTRAINT uk_customer_tags_customer_name UNIQUE (customer_id, name),
    CONSTRAINT fk_customer_tags_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_tags_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_customer_tags_name ON customer_tags (name);

CREATE TABLE customer_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(4000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_customer_notes PRIMARY KEY (id),
    CONSTRAINT fk_customer_notes_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_notes_author
        FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_customer_notes_customer_created
    ON customer_notes (customer_id, created_at);

CREATE TABLE customer_favorite_listings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    added_by BIGINT,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_customer_favorite_listings PRIMARY KEY (id),
    CONSTRAINT uk_customer_favorite_listings_customer_listing
        UNIQUE (customer_id, listing_id),
    CONSTRAINT fk_customer_favorite_listings_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_favorite_listings_listing
        FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_favorite_listings_added_by
        FOREIGN KEY (added_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_customer_favorite_listings_customer_created
    ON customer_favorite_listings (customer_id, created_at);
CREATE INDEX idx_customer_favorite_listings_listing
    ON customer_favorite_listings (listing_id);
