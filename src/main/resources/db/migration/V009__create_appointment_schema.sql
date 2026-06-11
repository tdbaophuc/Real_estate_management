CREATE TABLE appointments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    lead_id BIGINT,
    customer_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    listing_id BIGINT,
    created_by BIGINT NOT NULL,
    rescheduled_from_id BIGINT,
    title VARCHAR(250) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    meeting_location VARCHAR(500),
    notes VARCHAR(2000),
    cancellation_reason VARCHAR(1000),
    cancelled_by BIGINT,
    confirmed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    reminder_sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT uk_appointments_code UNIQUE (code),
    CONSTRAINT fk_appointments_lead
        FOREIGN KEY (lead_id) REFERENCES leads (id) ON DELETE SET NULL,
    CONSTRAINT fk_appointments_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_appointments_agent
        FOREIGN KEY (agent_id) REFERENCES users (id),
    CONSTRAINT fk_appointments_property
        FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_appointments_listing
        FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE SET NULL,
    CONSTRAINT fk_appointments_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_appointments_rescheduled_from
        FOREIGN KEY (rescheduled_from_id) REFERENCES appointments (id) ON DELETE SET NULL,
    CONSTRAINT fk_appointments_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_appointments_status CHECK (
        status IN (
            'PENDING',
            'CONFIRMED',
            'CANCELLED',
            'COMPLETED',
            'NO_SHOW',
            'RESCHEDULED'
        )
    ),
    CONSTRAINT ck_appointments_time CHECK (
        end_at > start_at
    ),
    CONSTRAINT ck_appointments_cancellation CHECK (
        (status = 'CANCELLED'
            AND cancellation_reason IS NOT NULL
            AND cancelled_at IS NOT NULL)
        OR (status <> 'CANCELLED')
    ),
    CONSTRAINT ck_appointments_completion CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR (status <> 'COMPLETED')
    ),
    CONSTRAINT ck_appointments_confirmation CHECK (
        (status = 'CONFIRMED' AND confirmed_at IS NOT NULL)
        OR (status <> 'CONFIRMED')
    )
);

CREATE INDEX idx_appointments_agent_time
    ON appointments (agent_id, start_at, end_at);
CREATE INDEX idx_appointments_property_time
    ON appointments (property_id, start_at, end_at);
CREATE INDEX idx_appointments_customer_time
    ON appointments (customer_id, start_at);
CREATE INDEX idx_appointments_status_start
    ON appointments (status, start_at);
CREATE INDEX idx_appointments_listing ON appointments (listing_id);
CREATE INDEX idx_appointments_lead ON appointments (lead_id);

CREATE TABLE appointment_participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    participant_role VARCHAR(30) NOT NULL,
    response_status VARCHAR(30) NOT NULL DEFAULT 'INVITED',
    responded_at TIMESTAMP NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointment_participants PRIMARY KEY (id),
    CONSTRAINT uk_appointment_participants_appointment_user
        UNIQUE (appointment_id, user_id),
    CONSTRAINT fk_appointment_participants_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id) ON DELETE CASCADE,
    CONSTRAINT fk_appointment_participants_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_appointment_participants_role CHECK (
        participant_role IN ('CUSTOMER', 'AGENT', 'OWNER', 'OTHER')
    ),
    CONSTRAINT ck_appointment_participants_response CHECK (
        response_status IN ('INVITED', 'ACCEPTED', 'DECLINED', 'TENTATIVE')
    ),
    CONSTRAINT ck_appointment_participants_responded CHECK (
        (response_status = 'INVITED' AND responded_at IS NULL)
        OR (response_status <> 'INVITED' AND responded_at IS NOT NULL)
    )
);

CREATE INDEX idx_appointment_participants_user_response
    ON appointment_participants (user_id, response_status);

CREATE TABLE viewing_feedbacks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    submitted_by BIGINT NOT NULL,
    rating INTEGER,
    interest_level VARCHAR(30) NOT NULL,
    comments VARCHAR(4000),
    positive_points VARCHAR(2000),
    concerns VARCHAR(2000),
    next_action VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_viewing_feedbacks PRIMARY KEY (id),
    CONSTRAINT uk_viewing_feedbacks_appointment_submitter
        UNIQUE (appointment_id, submitted_by),
    CONSTRAINT fk_viewing_feedbacks_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id) ON DELETE CASCADE,
    CONSTRAINT fk_viewing_feedbacks_submitted_by
        FOREIGN KEY (submitted_by) REFERENCES users (id),
    CONSTRAINT ck_viewing_feedbacks_rating CHECK (
        rating IS NULL OR (rating >= 1 AND rating <= 5)
    ),
    CONSTRAINT ck_viewing_feedbacks_interest CHECK (
        interest_level IN ('HIGH', 'MEDIUM', 'LOW', 'NOT_INTERESTED')
    )
);

CREATE INDEX idx_viewing_feedbacks_appointment_created
    ON viewing_feedbacks (appointment_id, created_at);
