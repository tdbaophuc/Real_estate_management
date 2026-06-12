CREATE INDEX IF NOT EXISTS idx_properties_deleted_status_created
    ON properties (deleted_at, status, created_at);

CREATE INDEX IF NOT EXISTS idx_properties_assigned_deleted_created
    ON properties (assigned_agent_id, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_properties_created_deleted_created
    ON properties (created_by, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_properties_type_status_deleted_price
    ON properties (property_type_id, status, deleted_at, price);

CREATE INDEX IF NOT EXISTS idx_listings_public_published
    ON listings (status, visibility, deleted_at, published_at, id);

CREATE INDEX IF NOT EXISTS idx_listings_public_purpose_price
    ON listings (status, visibility, purpose, deleted_at, asking_price);

CREATE INDEX IF NOT EXISTS idx_listings_created_deleted_created
    ON listings (created_by, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_listings_property_deleted_created
    ON listings (property_id, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_customers_agent_deleted_status_created
    ON customers (assigned_agent_id, deleted_at, status, created_at);

CREATE INDEX IF NOT EXISTS idx_customers_created_deleted_created
    ON customers (created_by, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_customers_deleted_status_priority
    ON customers (deleted_at, status, priority, created_at);

CREATE INDEX IF NOT EXISTS idx_leads_assignee_deleted_status_created
    ON leads (current_assignee_id, deleted_at, status, created_at);

CREATE INDEX IF NOT EXISTS idx_leads_created_deleted_created
    ON leads (created_by, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_leads_deleted_status_created
    ON leads (deleted_at, status, created_at);

CREATE INDEX IF NOT EXISTS idx_appointments_agent_status_start
    ON appointments (agent_id, status, start_at);

CREATE INDEX IF NOT EXISTS idx_transactions_agent_status_created
    ON transactions (agent_id, status, created_at);

CREATE INDEX IF NOT EXISTS idx_transactions_status_created
    ON transactions (status, created_at);

CREATE INDEX IF NOT EXISTS idx_transactions_completed_report
    ON transactions (status, completed_at, currency);

CREATE INDEX IF NOT EXISTS idx_payments_status_paid_report
    ON payments (status, paid_at, currency);

CREATE INDEX IF NOT EXISTS idx_deposits_status_verified_report
    ON deposits (status, verified_at, currency);

CREATE INDEX IF NOT EXISTS idx_commissions_status_paid_report
    ON commissions (status, paid_at, currency);

CREATE INDEX IF NOT EXISTS idx_commissions_created_report
    ON commissions (created_at, status, currency);
