 -- Real Estate Management demo data
-- Target database: PostgreSQL after Flyway migrations V001-V020.
-- Safe to run more than once for rows that have unique business codes/emails.

BEGIN;

-- Demo users. All accounts use the same bcrypt sample password hash.
-- Plain password for local demo reference: Password@123
INSERT INTO users (email, password_hash, full_name, phone, status, email_verified, last_login_at)
VALUES
    ('admin@realestate.demo', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiQE4Q2Iv5L3fvvEnLr7dCZPUmIeJ4e', 'Demo Administrator', '+84900000001', 'ACTIVE', TRUE, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    ('manager@realestate.demo', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiQE4Q2Iv5L3fvvEnLr7dCZPUmIeJ4e', 'Nguyen Minh Manager', '+84900000002', 'ACTIVE', TRUE, CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    ('agent.lan@realestate.demo', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiQE4Q2Iv5L3fvvEnLr7dCZPUmIeJ4e', 'Tran Ngoc Lan', '+84900000003', 'ACTIVE', TRUE, CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
    ('agent.khoa@realestate.demo', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiQE4Q2Iv5L3fvvEnLr7dCZPUmIeJ4e', 'Le Anh Khoa', '+84900000004', 'ACTIVE', TRUE, CURRENT_TIMESTAMP - INTERVAL '4 hours'),
    ('owner.hung@realestate.demo', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiQE4Q2Iv5L3fvvEnLr7dCZPUmIeJ4e', 'Pham Quoc Hung', '+84900000005', 'ACTIVE', TRUE, NULL),
    ('owner.mai@realestate.demo', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiQE4Q2Iv5L3fvvEnLr7dCZPUmIeJ4e', 'Do Thanh Mai', '+84900000006', 'ACTIVE', TRUE, NULL),
    ('customer.an@realestate.demo', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiQE4Q2Iv5L3fvvEnLr7dCZPUmIeJ4e', 'Hoang Gia An', '+84900000007', 'ACTIVE', TRUE, CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    ('customer.linh@realestate.demo', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiQE4Q2Iv5L3fvvEnLr7dCZPUmIeJ4e', 'Bui Thuy Linh', '+84900000008', 'ACTIVE', TRUE, CURRENT_TIMESTAMP - INTERVAL '5 hours')
ON CONFLICT (email) DO UPDATE
SET full_name = EXCLUDED.full_name,
    status = EXCLUDED.status,
    email_verified = EXCLUDED.email_verified,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = CASE
    WHEN u.email = 'admin@realestate.demo' THEN 'ADMIN'
    WHEN u.email = 'manager@realestate.demo' THEN 'MANAGER'
    WHEN u.email IN ('agent.lan@realestate.demo', 'agent.khoa@realestate.demo') THEN 'AGENT'
    WHEN u.email IN ('owner.hung@realestate.demo', 'owner.mai@realestate.demo') THEN 'OWNER'
    ELSE 'CUSTOMER'
END
WHERE u.email LIKE '%@realestate.demo'
ON CONFLICT DO NOTHING;

-- Location master data for Ho Chi Minh City demo inventory.
INSERT INTO provinces (code, name, administrative_type)
VALUES ('HCM', 'Ho Chi Minh City', 'Municipality')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, updated_at = CURRENT_TIMESTAMP;

INSERT INTO districts (province_id, code, name, administrative_type)
SELECT p.id, v.code, v.name, v.administrative_type
FROM provinces p
CROSS JOIN (VALUES
    ('HCM-D1', 'District 1', 'Urban district'),
    ('HCM-D2', 'Thu Duc City', 'City'),
    ('HCM-D7', 'District 7', 'Urban district')
) AS v(code, name, administrative_type)
WHERE p.code = 'HCM'
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, updated_at = CURRENT_TIMESTAMP;

INSERT INTO wards (district_id, code, name, administrative_type)
SELECT d.id, v.code, v.name, 'Ward'
FROM districts d
JOIN (VALUES
    ('HCM-D1', 'HCM-BN', 'Ben Nghe'),
    ('HCM-D2', 'HCM-AP', 'An Phu'),
    ('HCM-D7', 'HCM-TQ', 'Tan Quy')
) AS v(district_code, code, name) ON v.district_code = d.code
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, updated_at = CURRENT_TIMESTAMP;

INSERT INTO listing_packages (code, name, description, price, duration_days, featured, priority_level)
VALUES
    ('STANDARD_30', 'Standard 30 days', 'Regular public listing for 30 days', 0, 30, FALSE, 10),
    ('FEATURED_30', 'Featured 30 days', 'Featured placement for high-intent inventory', 1500000, 30, TRUE, 50)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    duration_days = EXCLUDED.duration_days,
    featured = EXCLUDED.featured,
    priority_level = EXCLUDED.priority_level,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO addresses (province_id, district_id, ward_id, street_address, full_address, latitude, longitude)
SELECT p.id, d.id, w.id, v.street_address, v.full_address, v.latitude, v.longitude
FROM (VALUES
    ('HCM-D1', 'HCM-BN', '45 Nguyen Hue Boulevard', '45 Nguyen Hue Boulevard, Ben Nghe, District 1, Ho Chi Minh City', 10.773108, 106.703140),
    ('HCM-D2', 'HCM-AP', '18 Mai Chi Tho Street', '18 Mai Chi Tho Street, An Phu, Thu Duc City, Ho Chi Minh City', 10.802719, 106.735493),
    ('HCM-D7', 'HCM-TQ', '88 Nguyen Thi Thap Street', '88 Nguyen Thi Thap Street, Tan Quy, District 7, Ho Chi Minh City', 10.740243, 106.710959)
) AS v(district_code, ward_code, street_address, full_address, latitude, longitude)
JOIN provinces p ON p.code = 'HCM'
JOIN districts d ON d.code = v.district_code
JOIN wards w ON w.code = v.ward_code
WHERE NOT EXISTS (
    SELECT 1 FROM addresses a WHERE a.full_address = v.full_address
);

INSERT INTO properties (
    code, property_type_id, address_id, owner_id, created_by, assigned_agent_id,
    name, description, purpose, status, price, land_area, floor_area,
    bedrooms, bathrooms, floors, direction, legal_status, furniture_status, available_from
)
SELECT v.code, pt.id, a.id, owner_user.id, creator.id, agent.id,
       v.name, v.description, v.purpose, v.status, v.price, v.land_area, v.floor_area,
       v.bedrooms, v.bathrooms, v.floors, v.direction, v.legal_status, v.furniture_status, v.available_from
FROM (VALUES
    ('PROP-DEMO-001', 'APARTMENT', '45 Nguyen Hue Boulevard, Ben Nghe, District 1, Ho Chi Minh City', 'owner.hung@realestate.demo', 'manager@realestate.demo', 'agent.lan@realestate.demo', 'Nguyen Hue Skyline Apartment', 'High-floor apartment with river view, concierge service and direct access to District 1 business amenities.', 'SALE', 'AVAILABLE', 12500000000, 92, 92, 2, 2, 1, 'EAST', 'PINK_BOOK', 'FULLY_FURNISHED', CURRENT_DATE),
    ('PROP-DEMO-002', 'VILLA', '18 Mai Chi Tho Street, An Phu, Thu Duc City, Ho Chi Minh City', 'owner.mai@realestate.demo', 'manager@realestate.demo', 'agent.khoa@realestate.demo', 'An Phu Garden Villa', 'Quiet compound villa with private garden, family living room and parking for two cars.', 'RENT', 'AVAILABLE', 85000000, 240, 310, 4, 4, 3, 'SOUTHEAST', 'PINK_BOOK', 'PARTIALLY_FURNISHED', CURRENT_DATE + 10),
    ('PROP-DEMO-003', 'OFFICE', '88 Nguyen Thi Thap Street, Tan Quy, District 7, Ho Chi Minh City', 'owner.hung@realestate.demo', 'manager@realestate.demo', 'agent.lan@realestate.demo', 'District 7 Flexible Office', 'Modern office floor for startup or representative office, close to Crescent Mall and main arterial roads.', 'RENT', 'AVAILABLE', 52000000, 160, 160, 0, 2, 1, 'NORTH', 'OTHER', 'UNFURNISHED', CURRENT_DATE + 3)
) AS v(code, type_code, full_address, owner_email, creator_email, agent_email, name, description, purpose, status, price, land_area, floor_area, bedrooms, bathrooms, floors, direction, legal_status, furniture_status, available_from)
JOIN property_types pt ON pt.code = v.type_code
JOIN addresses a ON a.full_address = v.full_address
JOIN users owner_user ON owner_user.email = v.owner_email
JOIN users creator ON creator.email = v.creator_email
JOIN users agent ON agent.email = v.agent_email
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    price = EXCLUDED.price,
    assigned_agent_id = EXCLUDED.assigned_agent_id,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO property_amenities (property_id, amenity_id, details)
SELECT p.id, a.id, v.details
FROM (VALUES
    ('PROP-DEMO-001', 'PARKING', 'One basement parking slot'),
    ('PROP-DEMO-001', 'ELEVATOR', 'Four high-speed elevators'),
    ('PROP-DEMO-001', 'SECURITY', '24/7 reception and access card'),
    ('PROP-DEMO-001', 'GYM', 'Resident fitness center'),
    ('PROP-DEMO-002', 'PARKING', 'Two covered car spaces'),
    ('PROP-DEMO-002', 'SWIMMING_POOL', 'Private pool in compound'),
    ('PROP-DEMO-002', 'AIR_CONDITIONING', 'Air conditioning in every room'),
    ('PROP-DEMO-003', 'ELEVATOR', 'Direct lift access'),
    ('PROP-DEMO-003', 'SECURITY', 'Building security desk')
) AS v(property_code, amenity_code, details)
JOIN properties p ON p.code = v.property_code
JOIN amenities a ON a.code = v.amenity_code
ON CONFLICT (property_id, amenity_id) DO UPDATE SET details = EXCLUDED.details;

INSERT INTO file_resources (uploaded_by, original_file_name, storage_key, content_type, file_size, checksum_sha256, storage_provider, access_level, public_url)
SELECT u.id, v.original_file_name, v.storage_key, v.content_type, v.file_size, v.checksum_sha256, 'LOCAL', v.access_level, v.public_url
FROM users u
JOIN (VALUES
    ('agent.lan@realestate.demo', 'prop-demo-001-cover.jpg', 'demo/properties/prop-demo-001-cover.jpg', 'image/jpeg', 248120, '1111111111111111111111111111111111111111111111111111111111111111', 'PUBLIC', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267'),
    ('agent.khoa@realestate.demo', 'prop-demo-002-cover.jpg', 'demo/properties/prop-demo-002-cover.jpg', 'image/jpeg', 310880, '2222222222222222222222222222222222222222222222222222222222222222', 'PUBLIC', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6'),
    ('agent.lan@realestate.demo', 'prop-demo-003-cover.jpg', 'demo/properties/prop-demo-003-cover.jpg', 'image/jpeg', 198450, '3333333333333333333333333333333333333333333333333333333333333333', 'PUBLIC', 'https://images.unsplash.com/photo-1497366754035-f200968a6e72'),
    ('manager@realestate.demo', 'contract-demo-001.pdf', 'demo/contracts/contract-demo-001.pdf', 'application/pdf', 84200, '4444444444444444444444444444444444444444444444444444444444444444', 'PRIVATE', NULL)
) AS v(email, original_file_name, storage_key, content_type, file_size, checksum_sha256, access_level, public_url) ON u.email = v.email
ON CONFLICT (storage_key) DO UPDATE
SET original_file_name = EXCLUDED.original_file_name,
    content_type = EXCLUDED.content_type,
    file_size = EXCLUDED.file_size,
    access_level = EXCLUDED.access_level,
    public_url = EXCLUDED.public_url;

INSERT INTO property_images (property_id, uploaded_by, file_resource_id, storage_key, image_url, file_name, mime_type, file_size, alt_text, cover_image, display_order)
SELECT p.id, u.id, fr.id, fr.storage_key, fr.public_url, fr.original_file_name, fr.content_type, fr.file_size, v.alt_text, TRUE, 1
FROM (VALUES
    ('PROP-DEMO-001', 'agent.lan@realestate.demo', 'demo/properties/prop-demo-001-cover.jpg', 'Living room and skyline view'),
    ('PROP-DEMO-002', 'agent.khoa@realestate.demo', 'demo/properties/prop-demo-002-cover.jpg', 'Garden villa exterior'),
    ('PROP-DEMO-003', 'agent.lan@realestate.demo', 'demo/properties/prop-demo-003-cover.jpg', 'Open-plan office workspace')
) AS v(property_code, uploader_email, storage_key, alt_text)
JOIN properties p ON p.code = v.property_code
JOIN users u ON u.email = v.uploader_email
JOIN file_resources fr ON fr.storage_key = v.storage_key
ON CONFLICT (storage_key) DO UPDATE
SET alt_text = EXCLUDED.alt_text,
    cover_image = EXCLUDED.cover_image,
    file_resource_id = EXCLUDED.file_resource_id;

INSERT INTO listings (
    code, property_id, created_by, reviewed_by, listing_package_id, title, slug, description,
    purpose, status, visibility, asking_price, seo_title, seo_description, seo_keywords,
    submitted_at, reviewed_at, published_at, expires_at, featured_until, view_count
)
SELECT v.code, p.id, creator.id, reviewer.id, lp.id, v.title, v.slug, v.description,
       v.purpose, v.status, 'PUBLIC', v.asking_price, v.seo_title, v.seo_description, v.seo_keywords,
       CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '5 days',
       CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '25 days',
       v.featured_until, v.view_count
FROM (VALUES
    ('LIST-DEMO-001', 'PROP-DEMO-001', 'agent.lan@realestate.demo', 'manager@realestate.demo', 'FEATURED_30', 'Nguyen Hue Skyline Apartment for Sale', 'nguyen-hue-skyline-apartment-sale-demo', 'Premium District 1 apartment ready for transfer with bright living room, two bedrooms and a strong rental profile.', 'SALE', 'PUBLISHED', 12500000000, 'District 1 skyline apartment for sale', 'Two-bedroom apartment on Nguyen Hue with full furniture and verified ownership documents.', 'district 1, apartment, sale, nguyen hue', CURRENT_TIMESTAMP + INTERVAL '10 days', 184),
    ('LIST-DEMO-002', 'PROP-DEMO-002', 'agent.khoa@realestate.demo', 'manager@realestate.demo', 'FEATURED_30', 'An Phu Garden Villa for Rent', 'an-phu-garden-villa-rent-demo', 'Family villa in a secure An Phu compound with garden, pool access and flexible lease terms.', 'RENT', 'PUBLISHED', 85000000, 'An Phu villa for rent', 'Four-bedroom villa for rent in An Phu with private garden and parking.', 'an phu, villa, rent, thu duc', CURRENT_TIMESTAMP + INTERVAL '12 days', 96),
    ('LIST-DEMO-003', 'PROP-DEMO-003', 'agent.lan@realestate.demo', 'manager@realestate.demo', 'STANDARD_30', 'District 7 Flexible Office for Lease', 'district-7-flexible-office-lease-demo', 'Efficient office floor suitable for 25 to 35 staff, available immediately after fit-out handover.', 'RENT', 'PUBLISHED', 52000000, 'District 7 office for lease', 'Flexible office near Nguyen Thi Thap and Crescent Mall.', 'district 7, office, lease', NULL, 61)
) AS v(code, property_code, creator_email, reviewer_email, package_code, title, slug, description, purpose, status, asking_price, seo_title, seo_description, seo_keywords, featured_until, view_count)
JOIN properties p ON p.code = v.property_code
JOIN users creator ON creator.email = v.creator_email
JOIN users reviewer ON reviewer.email = v.reviewer_email
JOIN listing_packages lp ON lp.code = v.package_code
ON CONFLICT (code) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    asking_price = EXCLUDED.asking_price,
    view_count = EXCLUDED.view_count,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO listing_status_histories (listing_id, from_status, to_status, changed_by, reason)
SELECT l.id, v.from_status, v.to_status, u.id, v.reason
FROM (VALUES
    ('LIST-DEMO-001', NULL, 'PENDING_REVIEW', 'agent.lan@realestate.demo', 'Submitted for manager review'),
    ('LIST-DEMO-001', 'PENDING_REVIEW', 'PUBLISHED', 'manager@realestate.demo', 'Approved for public demo'),
    ('LIST-DEMO-002', NULL, 'PENDING_REVIEW', 'agent.khoa@realestate.demo', 'Submitted for manager review'),
    ('LIST-DEMO-002', 'PENDING_REVIEW', 'PUBLISHED', 'manager@realestate.demo', 'Approved for public demo')
) AS v(listing_code, from_status, to_status, user_email, reason)
JOIN listings l ON l.code = v.listing_code
JOIN users u ON u.email = v.user_email
WHERE NOT EXISTS (
    SELECT 1 FROM listing_status_histories h
    WHERE h.listing_id = l.id AND h.to_status = v.to_status AND h.reason = v.reason
);

INSERT INTO customers (code, user_id, assigned_agent_id, created_by, full_name, email, phone, source, priority, preferred_contact_method, notes)
SELECT v.code, customer_user.id, agent.id, creator.id, customer_user.full_name, customer_user.email, customer_user.phone, v.source, v.priority, v.preferred_contact_method, v.notes
FROM (VALUES
    ('CUS-DEMO-001', 'customer.an@realestate.demo', 'agent.lan@realestate.demo', 'manager@realestate.demo', 'WEBSITE', 'HIGH', 'PHONE', 'Looking for a central apartment for own use and long-term value.'),
    ('CUS-DEMO-002', 'customer.linh@realestate.demo', 'agent.khoa@realestate.demo', 'manager@realestate.demo', 'REFERRAL', 'MEDIUM', 'EMAIL', 'Relocating family needs a quiet villa near international schools.')
) AS v(code, customer_email, agent_email, creator_email, source, priority, preferred_contact_method, notes)
JOIN users customer_user ON customer_user.email = v.customer_email
JOIN users agent ON agent.email = v.agent_email
JOIN users creator ON creator.email = v.creator_email
ON CONFLICT (code) DO UPDATE
SET assigned_agent_id = EXCLUDED.assigned_agent_id,
    priority = EXCLUDED.priority,
    notes = EXCLUDED.notes,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO customer_requirements (customer_id, property_type_id, province_id, district_id, purpose, min_budget, max_budget, min_area, max_area, min_bedrooms, min_bathrooms, description)
SELECT c.id, pt.id, p.id, d.id, v.purpose, v.min_budget, v.max_budget, v.min_area, v.max_area, v.min_bedrooms, v.min_bathrooms, v.description
FROM (VALUES
    ('CUS-DEMO-001', 'APARTMENT', 'HCM-D1', 'SALE', 9000000000, 14000000000, 70, 110, 2, 2, 'Central two-bedroom apartment with clean legal documents.'),
    ('CUS-DEMO-002', 'VILLA', 'HCM-D2', 'RENT', 60000000, 100000000, 220, 350, 4, 3, 'Family villa with garden, parking and easy school commute.')
) AS v(customer_code, type_code, district_code, purpose, min_budget, max_budget, min_area, max_area, min_bedrooms, min_bathrooms, description)
JOIN customers c ON c.code = v.customer_code
JOIN property_types pt ON pt.code = v.type_code
JOIN provinces p ON p.code = 'HCM'
JOIN districts d ON d.code = v.district_code
WHERE NOT EXISTS (
    SELECT 1 FROM customer_requirements cr WHERE cr.customer_id = c.id AND cr.description = v.description
);

INSERT INTO customer_tags (customer_id, name, color, created_by)
SELECT c.id, v.name, v.color, u.id
FROM (VALUES
    ('CUS-DEMO-001', 'Hot buyer', '#D92D20', 'agent.lan@realestate.demo'),
    ('CUS-DEMO-002', 'Relocation', '#1570EF', 'agent.khoa@realestate.demo')
) AS v(customer_code, name, color, user_email)
JOIN customers c ON c.code = v.customer_code
JOIN users u ON u.email = v.user_email
ON CONFLICT (customer_id, name) DO UPDATE SET color = EXCLUDED.color;

INSERT INTO customer_notes (customer_id, author_id, content, pinned)
SELECT c.id, u.id, v.content, v.pinned
FROM (VALUES
    ('CUS-DEMO-001', 'agent.lan@realestate.demo', 'Customer is available for viewing after 18:00 on weekdays.', TRUE),
    ('CUS-DEMO-002', 'agent.khoa@realestate.demo', 'Prefers quiet compound and minimum four bedrooms.', FALSE)
) AS v(customer_code, user_email, content, pinned)
JOIN customers c ON c.code = v.customer_code
JOIN users u ON u.email = v.user_email
WHERE NOT EXISTS (
    SELECT 1 FROM customer_notes n WHERE n.customer_id = c.id AND n.content = v.content
);

INSERT INTO customer_favorite_listings (customer_id, listing_id, added_by, notes)
SELECT c.id, l.id, u.id, v.notes
FROM (VALUES
    ('CUS-DEMO-001', 'LIST-DEMO-001', 'agent.lan@realestate.demo', 'Matches budget and preferred District 1 location.'),
    ('CUS-DEMO-002', 'LIST-DEMO-002', 'agent.khoa@realestate.demo', 'Strong match for family relocation requirement.')
) AS v(customer_code, listing_code, user_email, notes)
JOIN customers c ON c.code = v.customer_code
JOIN listings l ON l.code = v.listing_code
JOIN users u ON u.email = v.user_email
ON CONFLICT (customer_id, listing_id) DO UPDATE SET notes = EXCLUDED.notes;

INSERT INTO leads (code, customer_id, source_id, listing_id, current_assignee_id, created_by, full_name, email, phone, status, priority, score, message, last_contacted_at)
SELECT v.code, c.id, ls.id, l.id, agent.id, creator.id, c.full_name, c.email, c.phone, v.status, v.priority, v.score, v.message, CURRENT_TIMESTAMP - v.last_contact_interval
FROM (VALUES
    ('LEAD-DEMO-001', 'CUS-DEMO-001', 'LISTING_INQUIRY', 'LIST-DEMO-001', 'agent.lan@realestate.demo', 'manager@realestate.demo', 'NEGOTIATING', 'HIGH', 91, 'Customer requested viewing and payment timeline for Nguyen Hue apartment.', INTERVAL '2 hours'),
    ('LEAD-DEMO-002', 'CUS-DEMO-002', 'REFERRAL', 'LIST-DEMO-002', 'agent.khoa@realestate.demo', 'manager@realestate.demo', 'VIEWING_SCHEDULED', 'MEDIUM', 76, 'Referred by corporate HR partner for family villa lease.', INTERVAL '1 day')
) AS v(code, customer_code, source_code, listing_code, agent_email, creator_email, status, priority, score, message, last_contact_interval)
JOIN customers c ON c.code = v.customer_code
JOIN lead_sources ls ON ls.code = v.source_code
JOIN listings l ON l.code = v.listing_code
JOIN users agent ON agent.email = v.agent_email
JOIN users creator ON creator.email = v.creator_email
ON CONFLICT (code) DO UPDATE
SET status = EXCLUDED.status,
    priority = EXCLUDED.priority,
    score = EXCLUDED.score,
    message = EXCLUDED.message,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO lead_assignments (lead_id, assigned_to, assigned_by, notes)
SELECT lead.id, agent.id, manager_user.id, v.notes
FROM (VALUES
    ('LEAD-DEMO-001', 'agent.lan@realestate.demo', 'manager@realestate.demo', 'Assigned to listing owner agent.'),
    ('LEAD-DEMO-002', 'agent.khoa@realestate.demo', 'manager@realestate.demo', 'Assigned due to villa market experience.')
) AS v(lead_code, agent_email, manager_email, notes)
JOIN leads lead ON lead.code = v.lead_code
JOIN users agent ON agent.email = v.agent_email
JOIN users manager_user ON manager_user.email = v.manager_email
WHERE NOT EXISTS (
    SELECT 1 FROM lead_assignments la WHERE la.lead_id = lead.id AND la.assigned_to = agent.id AND la.active = TRUE
);

INSERT INTO lead_notes (lead_id, author_id, content, pinned)
SELECT lead.id, u.id, v.content, v.pinned
FROM (VALUES
    ('LEAD-DEMO-001', 'agent.lan@realestate.demo', 'Prepare ownership document summary before next negotiation call.', TRUE),
    ('LEAD-DEMO-002', 'agent.khoa@realestate.demo', 'Viewing confirmed with customer and spouse.', FALSE)
) AS v(lead_code, user_email, content, pinned)
JOIN leads lead ON lead.code = v.lead_code
JOIN users u ON u.email = v.user_email
WHERE NOT EXISTS (
    SELECT 1 FROM lead_notes n WHERE n.lead_id = lead.id AND n.content = v.content
);

INSERT INTO lead_activities (lead_id, actor_id, activity_type, subject, details, occurred_at)
SELECT lead.id, u.id, v.activity_type, v.subject, v.details, CURRENT_TIMESTAMP - v.age
FROM (VALUES
    ('LEAD-DEMO-001', 'agent.lan@realestate.demo', 'CALL', 'Budget qualification', 'Confirmed bank pre-approval and target closing in 45 days.', INTERVAL '2 hours'),
    ('LEAD-DEMO-002', 'agent.khoa@realestate.demo', 'MEETING', 'Viewing scheduled', 'Customer accepted Saturday morning viewing slot.', INTERVAL '4 hours')
) AS v(lead_code, user_email, activity_type, subject, details, age)
JOIN leads lead ON lead.code = v.lead_code
JOIN users u ON u.email = v.user_email
WHERE NOT EXISTS (
    SELECT 1 FROM lead_activities a WHERE a.lead_id = lead.id AND a.subject = v.subject
);

INSERT INTO follow_up_tasks (lead_id, assigned_to, created_by, title, description, status, priority, due_at)
SELECT lead.id, agent.id, manager_user.id, v.title, v.description, v.status, v.priority, CURRENT_TIMESTAMP + v.due_interval
FROM (VALUES
    ('LEAD-DEMO-001', 'agent.lan@realestate.demo', 'manager@realestate.demo', 'Send payment milestone options', 'Share deposit and notarization timeline with customer.', 'IN_PROGRESS', 'HIGH', INTERVAL '1 day'),
    ('LEAD-DEMO-002', 'agent.khoa@realestate.demo', 'manager@realestate.demo', 'Confirm villa viewing logistics', 'Confirm access card, parking and owner availability.', 'PENDING', 'MEDIUM', INTERVAL '2 days')
) AS v(lead_code, agent_email, manager_email, title, description, status, priority, due_interval)
JOIN leads lead ON lead.code = v.lead_code
JOIN users agent ON agent.email = v.agent_email
JOIN users manager_user ON manager_user.email = v.manager_email
WHERE NOT EXISTS (
    SELECT 1 FROM follow_up_tasks t WHERE t.lead_id = lead.id AND t.title = v.title
);

INSERT INTO appointments (code, lead_id, customer_id, agent_id, property_id, listing_id, created_by, title, status, start_at, end_at, meeting_location, notes, confirmed_at, completed_at)
SELECT v.code, lead.id, c.id, agent.id, p.id, l.id, creator.id, v.title, v.status,
       CURRENT_TIMESTAMP + v.start_interval,
       CURRENT_TIMESTAMP + v.end_interval,
       v.meeting_location, v.notes,
       CASE WHEN v.status = 'CONFIRMED' THEN CURRENT_TIMESTAMP - INTERVAL '1 hour' ELSE NULL END,
       CASE WHEN v.status = 'COMPLETED' THEN CURRENT_TIMESTAMP - INTERVAL '3 days' ELSE NULL END
FROM (VALUES
    ('APT-DEMO-001', 'LEAD-DEMO-001', 'CUS-DEMO-001', 'agent.lan@realestate.demo', 'PROP-DEMO-001', 'LIST-DEMO-001', 'agent.lan@realestate.demo', 'Nguyen Hue apartment second viewing', 'CONFIRMED', INTERVAL '1 day', INTERVAL '1 day 1 hour', 'Lobby reception, 45 Nguyen Hue Boulevard', 'Bring parking card and building registration form.'),
    ('APT-DEMO-002', 'LEAD-DEMO-002', 'CUS-DEMO-002', 'agent.khoa@realestate.demo', 'PROP-DEMO-002', 'LIST-DEMO-002', 'agent.khoa@realestate.demo', 'An Phu villa family viewing', 'CONFIRMED', INTERVAL '2 days', INTERVAL '2 days 90 minutes', 'Compound security gate, 18 Mai Chi Tho Street', 'Owner will open garden and storage room.')
) AS v(code, lead_code, customer_code, agent_email, property_code, listing_code, creator_email, title, status, start_interval, end_interval, meeting_location, notes)
JOIN leads lead ON lead.code = v.lead_code
JOIN customers c ON c.code = v.customer_code
JOIN users agent ON agent.email = v.agent_email
JOIN properties p ON p.code = v.property_code
JOIN listings l ON l.code = v.listing_code
JOIN users creator ON creator.email = v.creator_email
ON CONFLICT (code) DO UPDATE
SET title = EXCLUDED.title,
    status = EXCLUDED.status,
    start_at = EXCLUDED.start_at,
    end_at = EXCLUDED.end_at,
    notes = EXCLUDED.notes,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO appointment_participants (appointment_id, user_id, participant_role, response_status, responded_at, notes)
SELECT a.id, u.id, v.participant_role, 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '30 minutes', v.notes
FROM (VALUES
    ('APT-DEMO-001', 'customer.an@realestate.demo', 'CUSTOMER', 'Customer confirmed by phone'),
    ('APT-DEMO-001', 'agent.lan@realestate.demo', 'AGENT', 'Agent confirmed calendar'),
    ('APT-DEMO-002', 'customer.linh@realestate.demo', 'CUSTOMER', 'Customer confirmed by email'),
    ('APT-DEMO-002', 'agent.khoa@realestate.demo', 'AGENT', 'Agent confirmed calendar')
) AS v(appointment_code, user_email, participant_role, notes)
JOIN appointments a ON a.code = v.appointment_code
JOIN users u ON u.email = v.user_email
ON CONFLICT (appointment_id, user_id) DO UPDATE
SET response_status = EXCLUDED.response_status,
    responded_at = EXCLUDED.responded_at,
    notes = EXCLUDED.notes,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO contract_templates (code, name, contract_type, version, content_template, description, created_by)
SELECT v.code, v.name, v.contract_type, 1, v.content_template, v.description, u.id
FROM (VALUES
    ('SALE_STANDARD_DEMO', 'Standard sale contract demo', 'SALE', 'Sale contract between {{seller}} and {{buyer}} for {{property}}.', 'Demo template for residential sale contracts.'),
    ('LEASE_STANDARD_DEMO', 'Standard lease contract demo', 'LEASE', 'Lease contract between {{landlord}} and {{tenant}} for {{property}}.', 'Demo template for residential lease contracts.')
) AS v(code, name, contract_type, content_template, description)
JOIN users u ON u.email = 'manager@realestate.demo'
ON CONFLICT (code, version) DO UPDATE
SET name = EXCLUDED.name,
    content_template = EXCLUDED.content_template,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO contracts (code, template_id, property_id, customer_id, owner_id, agent_id, created_by, contract_type, status, title, total_value, effective_date, expiration_date, terms, notes, submitted_at, approved_at, signed_at, activated_at)
SELECT v.code, ct.id, p.id, c.id, owner_user.id, agent.id, creator.id, v.contract_type, v.status, v.title, v.total_value,
       CURRENT_DATE, v.expiration_date, v.terms, v.notes,
       CURRENT_TIMESTAMP - INTERVAL '4 days',
       CURRENT_TIMESTAMP - INTERVAL '3 days',
       CURRENT_TIMESTAMP - INTERVAL '2 days',
       CASE WHEN v.status = 'ACTIVE' THEN CURRENT_TIMESTAMP - INTERVAL '2 days' ELSE NULL END
FROM (VALUES
    ('CON-DEMO-001', 'SALE_STANDARD_DEMO', 'PROP-DEMO-001', 'CUS-DEMO-001', 'owner.hung@realestate.demo', 'agent.lan@realestate.demo', 'manager@realestate.demo', 'SALE', 'ACTIVE', 'Sale contract - Nguyen Hue Skyline Apartment', 12300000000, NULL::date, 'Deposit 10%, notarization within 30 days, remaining balance on title transfer.', 'Customer negotiated final price below asking.')
) AS v(code, template_code, property_code, customer_code, owner_email, agent_email, creator_email, contract_type, status, title, total_value, expiration_date, terms, notes)
JOIN contract_templates ct ON ct.code = v.template_code AND ct.version = 1
JOIN properties p ON p.code = v.property_code
JOIN customers c ON c.code = v.customer_code
JOIN users owner_user ON owner_user.email = v.owner_email
JOIN users agent ON agent.email = v.agent_email
JOIN users creator ON creator.email = v.creator_email
ON CONFLICT (code) DO UPDATE
SET status = EXCLUDED.status,
    total_value = EXCLUDED.total_value,
    terms = EXCLUDED.terms,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO contract_parties (contract_id, user_id, customer_id, party_role, full_name, email, phone, identity_number, address, signing_order)
SELECT con.id, party_user.id, c.id, v.party_role, v.full_name, v.email, v.phone, v.identity_number, v.address, v.signing_order
FROM (VALUES
    ('CON-DEMO-001', 'owner.hung@realestate.demo', NULL, 'SELLER', 'Pham Quoc Hung', 'owner.hung@realestate.demo', '+84900000005', '079088000001', 'Ho Chi Minh City', 1),
    ('CON-DEMO-001', 'customer.an@realestate.demo', 'CUS-DEMO-001', 'BUYER', 'Hoang Gia An', 'customer.an@realestate.demo', '+84900000007', '079099000002', 'District 1, Ho Chi Minh City', 2),
    ('CON-DEMO-001', 'agent.lan@realestate.demo', NULL, 'AGENT', 'Tran Ngoc Lan', 'agent.lan@realestate.demo', '+84900000003', NULL, 'Company office', 3)
) AS v(contract_code, user_email, customer_code, party_role, full_name, email, phone, identity_number, address, signing_order)
JOIN contracts con ON con.code = v.contract_code
LEFT JOIN users party_user ON party_user.email = v.user_email
LEFT JOIN customers c ON c.code = v.customer_code
WHERE NOT EXISTS (
    SELECT 1 FROM contract_parties cp WHERE cp.contract_id = con.id AND cp.party_role = v.party_role AND cp.email = v.email
);

INSERT INTO contract_documents (contract_id, file_resource_id, uploaded_by, document_type, version, display_name, description, primary_document)
SELECT con.id, fr.id, u.id, 'SIGNED', 1, 'Signed sale contract - Nguyen Hue Skyline Apartment', 'Demo signed contract PDF resource', TRUE
FROM contracts con
JOIN file_resources fr ON fr.storage_key = 'demo/contracts/contract-demo-001.pdf'
JOIN users u ON u.email = 'manager@realestate.demo'
WHERE con.code = 'CON-DEMO-001'
ON CONFLICT (file_resource_id) DO UPDATE
SET display_name = EXCLUDED.display_name,
    primary_document = EXCLUDED.primary_document;

INSERT INTO contract_signatures (contract_party_id, contract_document_id, signer_user_id, signature_method, status, provider_name, provider_signature_id, signature_data, signed_at, ip_address, user_agent)
SELECT cp.id, cd.id, cp.user_id, 'ELECTRONIC', 'SIGNED', 'DemoSign', 'demo-sign-' || cp.id, 'Signed through demo workflow', CURRENT_TIMESTAMP - INTERVAL '2 days', '127.0.0.1', 'Demo browser'
FROM contract_parties cp
JOIN contracts con ON con.id = cp.contract_id AND con.code = 'CON-DEMO-001'
JOIN contract_documents cd ON cd.contract_id = con.id AND cd.document_type = 'SIGNED'
WHERE cp.party_role IN ('SELLER', 'BUYER', 'AGENT')
ON CONFLICT (contract_party_id, contract_document_id) DO UPDATE
SET status = EXCLUDED.status,
    signed_at = EXCLUDED.signed_at,
    signature_data = EXCLUDED.signature_data,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO transactions (code, contract_id, property_id, customer_id, owner_id, agent_id, created_by, transaction_type, status, agreed_value, transaction_date, expected_completion_date, notes)
SELECT 'TX-DEMO-001', con.id, p.id, c.id, owner_user.id, agent.id, creator.id, 'SALE', 'PAYMENT_IN_PROGRESS', 12300000000, CURRENT_DATE - 2, CURRENT_DATE + 28, 'Demo active sale transaction with deposit verified and balance scheduled.'
FROM contracts con
JOIN properties p ON p.code = 'PROP-DEMO-001'
JOIN customers c ON c.code = 'CUS-DEMO-001'
JOIN users owner_user ON owner_user.email = 'owner.hung@realestate.demo'
JOIN users agent ON agent.email = 'agent.lan@realestate.demo'
JOIN users creator ON creator.email = 'manager@realestate.demo'
WHERE con.code = 'CON-DEMO-001'
ON CONFLICT (code) DO UPDATE
SET status = EXCLUDED.status,
    agreed_value = EXCLUDED.agreed_value,
    notes = EXCLUDED.notes,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO payment_schedules (transaction_id, installment_number, label, due_date, amount, paid_amount, status, paid_at, notes)
SELECT tx.id, v.installment_number, v.label, CURRENT_DATE + v.due_offset, v.amount, v.paid_amount, v.status,
       CASE WHEN v.status = 'PAID' THEN CURRENT_TIMESTAMP - INTERVAL '1 day' ELSE NULL END,
       v.notes
FROM transactions tx
JOIN (VALUES
    (1, 'Deposit 10%', -1, 1230000000, 1230000000, 'PAID', 'Verified bank transfer deposit.'),
    (2, 'Final balance at notarization', 28, 11070000000, 0, 'PENDING', 'Due when title transfer package is ready.')
) AS v(installment_number, label, due_offset, amount, paid_amount, status, notes) ON TRUE
WHERE tx.code = 'TX-DEMO-001'
ON CONFLICT (transaction_id, installment_number) DO UPDATE
SET label = EXCLUDED.label,
    amount = EXCLUDED.amount,
    paid_amount = EXCLUDED.paid_amount,
    status = EXCLUDED.status,
    paid_at = EXCLUDED.paid_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO deposits (transaction_id, received_by, amount, payment_method, status, reference_number, idempotency_key, due_date, received_at, verified_at, notes)
SELECT tx.id, u.id, 1230000000, 'BANK_TRANSFER', 'VERIFIED', 'VCB-DEMO-DEP-001', 'deposit-demo-tx-001', CURRENT_DATE - 1, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '20 hours', 'Deposit confirmed from customer bank transfer.'
FROM transactions tx
JOIN users u ON u.email = 'manager@realestate.demo'
WHERE tx.code = 'TX-DEMO-001'
ON CONFLICT (idempotency_key) DO UPDATE
SET status = EXCLUDED.status,
    reference_number = EXCLUDED.reference_number,
    verified_at = EXCLUDED.verified_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO payments (transaction_id, payment_schedule_id, received_by, amount, payment_method, status, reference_number, idempotency_key, paid_at, confirmed_at, notes)
SELECT tx.id, ps.id, u.id, 1230000000, 'BANK_TRANSFER', 'COMPLETED', 'VCB-DEMO-PAY-001', 'payment-demo-tx-001-installment-1', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '20 hours', 'Payment attached to deposit installment.'
FROM transactions tx
JOIN payment_schedules ps ON ps.transaction_id = tx.id AND ps.installment_number = 1
JOIN users u ON u.email = 'manager@realestate.demo'
WHERE tx.code = 'TX-DEMO-001'
ON CONFLICT (idempotency_key) DO UPDATE
SET status = EXCLUDED.status,
    reference_number = EXCLUDED.reference_number,
    confirmed_at = EXCLUDED.confirmed_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO invoices (transaction_id, issued_by, invoice_number, status, issue_date, due_date, subtotal, tax_amount, total_amount, billed_to_name, billed_to_email, billed_to_address, paid_at, notes)
SELECT tx.id, u.id, 'INV-DEMO-001', 'PAID', CURRENT_DATE - 1, CURRENT_DATE, 1230000000, 0, 1230000000, c.full_name, c.email, 'District 1, Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '20 hours', 'Deposit invoice for demo sale transaction.'
FROM transactions tx
JOIN customers c ON c.code = 'CUS-DEMO-001'
JOIN users u ON u.email = 'manager@realestate.demo'
WHERE tx.code = 'TX-DEMO-001'
ON CONFLICT (invoice_number) DO UPDATE
SET status = EXCLUDED.status,
    total_amount = EXCLUDED.total_amount,
    paid_at = EXCLUDED.paid_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO receipts (payment_id, issued_by, receipt_number, issued_at, amount, payer_name, notes)
SELECT pay.id, u.id, 'REC-DEMO-001', CURRENT_TIMESTAMP - INTERVAL '20 hours', pay.amount, c.full_name, 'Receipt for verified deposit payment.'
FROM payments pay
JOIN transactions tx ON tx.id = pay.transaction_id AND tx.code = 'TX-DEMO-001'
JOIN customers c ON c.id = tx.customer_id
JOIN users u ON u.email = 'manager@realestate.demo'
ON CONFLICT (receipt_number) DO UPDATE
SET amount = EXCLUDED.amount,
    notes = EXCLUDED.notes;

INSERT INTO commission_rules (code, name, transaction_type, calculation_type, rate, currency, min_transaction_value, priority, effective_from, description, created_by)
SELECT 'SALE_STANDARD_2PCT_DEMO', 'Sale commission 2 percent demo', 'SALE', 'PERCENTAGE', 2.0000, 'VND', 0, 100, CURRENT_DATE - 365, 'Demo rule for agent sale commission.', u.id
FROM users u
WHERE u.email = 'manager@realestate.demo'
ON CONFLICT (code) DO UPDATE
SET rate = EXCLUDED.rate,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO commissions (transaction_id, commission_rule_id, beneficiary_user_id, status, base_amount, rate, amount, approved_by, approved_at, notes)
SELECT tx.id, cr.id, agent.id, 'APPROVED', tx.agreed_value, cr.rate, ROUND(tx.agreed_value * cr.rate / 100, 2), manager_user.id, CURRENT_TIMESTAMP - INTERVAL '12 hours', 'Approved demo commission for agent.'
FROM transactions tx
JOIN commission_rules cr ON cr.code = 'SALE_STANDARD_2PCT_DEMO'
JOIN users agent ON agent.email = 'agent.lan@realestate.demo'
JOIN users manager_user ON manager_user.email = 'manager@realestate.demo'
WHERE tx.code = 'TX-DEMO-001'
ON CONFLICT (transaction_id, beneficiary_user_id) DO UPDATE
SET status = EXCLUDED.status,
    amount = EXCLUDED.amount,
    approved_by = EXCLUDED.approved_by,
    approved_at = EXCLUDED.approved_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO notifications (recipient_id, template_id, notification_type, title, message, action_url, reference_type, reference_id, metadata_json)
SELECT recipient.id, nt.id, v.notification_type, v.title, v.message, v.action_url, v.reference_type, ref.id, v.metadata_json
FROM (VALUES
    ('agent.lan@realestate.demo', 'APPOINTMENT_REMINDER_EMAIL', 'APPOINTMENT_REMINDER', 'Upcoming viewing confirmed', 'Nguyen Hue apartment second viewing is confirmed for tomorrow.', '/appointments/APT-DEMO-001', 'APPOINTMENT', 'APT-DEMO-001', '{"channel":"demo"}'),
    ('manager@realestate.demo', NULL, 'COMMISSION_APPROVED', 'Commission ready for payment', 'Sale commission for TX-DEMO-001 has been approved.', '/commissions', 'TRANSACTION', 'TX-DEMO-001', '{"amount":246000000}')
) AS v(recipient_email, template_code, notification_type, title, message, action_url, reference_type, reference_code, metadata_json)
JOIN users recipient ON recipient.email = v.recipient_email
LEFT JOIN notification_templates nt ON nt.code = v.template_code
LEFT JOIN appointments ref_apt ON v.reference_type = 'APPOINTMENT' AND ref_apt.code = v.reference_code
LEFT JOIN transactions ref_tx ON v.reference_type = 'TRANSACTION' AND ref_tx.code = v.reference_code
CROSS JOIN LATERAL (SELECT COALESCE(ref_apt.id, ref_tx.id) AS id) ref
WHERE NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.recipient_id = recipient.id AND n.notification_type = v.notification_type AND n.reference_type = v.reference_type AND n.reference_id = ref.id
);

INSERT INTO email_logs (template_id, recipient_user_id, recipient_email, subject, body, status, provider_message_id, attempt_count, scheduled_at, sent_at, reference_type, reference_id)
SELECT nt.id, u.id, u.email, 'Reminder: Nguyen Hue apartment second viewing', 'Your viewing is confirmed. Please arrive 10 minutes early at the lobby reception.', 'SENT', 'demo-email-apt-001', 1, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '50 minutes', 'APPOINTMENT', a.id
FROM notification_templates nt
JOIN users u ON u.email = 'customer.an@realestate.demo'
JOIN appointments a ON a.code = 'APT-DEMO-001'
WHERE nt.code = 'APPOINTMENT_REMINDER_EMAIL'
ON CONFLICT (template_id, recipient_email, reference_type, reference_id) DO UPDATE
SET status = EXCLUDED.status,
    sent_at = EXCLUDED.sent_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO ai_request_logs (provider, model, operation, system_prompt, user_prompt, metadata_json, response_content, status, finish_reason, prompt_tokens, completion_tokens, total_tokens, latency_ms, reference_type, reference_id)
SELECT 'noop', 'demo-local', 'LISTING_DESCRIPTION', 'Generate concise listing copy.', 'Create a listing description for Nguyen Hue Skyline Apartment.', '{"mode":"demo"}', 'Premium District 1 apartment with skyline views and verified ownership.', 'SUCCESS', 'stop', 120, 38, 158, 42, 'LISTING', l.id
FROM listings l
WHERE l.code = 'LIST-DEMO-001'
  AND NOT EXISTS (
      SELECT 1 FROM ai_request_logs ar WHERE ar.operation = 'LISTING_DESCRIPTION' AND ar.reference_type = 'LISTING' AND ar.reference_id = l.id
  );

INSERT INTO ai_recommendations (customer_id, listing_id, generated_by, match_score, reason, suggested_action, fallback_used, ai_status, provider, model)
SELECT c.id, l.id, u.id, 94, 'Budget, location and bedroom count align with the customer requirement.', 'Invite customer to second viewing and prepare contract FAQ.', FALSE, 'SUCCESS', 'noop', 'demo-local'
FROM customers c
JOIN listings l ON l.code = 'LIST-DEMO-001'
JOIN users u ON u.email = 'agent.lan@realestate.demo'
WHERE c.code = 'CUS-DEMO-001'
  AND NOT EXISTS (
      SELECT 1 FROM ai_recommendations r WHERE r.customer_id = c.id AND r.listing_id = l.id
  );

INSERT INTO ai_lead_scores (lead_id, generated_by, score, priority, reason, suggested_follow_up, fallback_used, ai_status, provider, model)
SELECT lead.id, u.id, 91, 'HIGH', 'Customer has verified budget, specific property interest and active negotiation timeline.', 'Send payment milestone options and request preferred notary date.', FALSE, 'SUCCESS', 'noop', 'demo-local'
FROM leads lead
JOIN users u ON u.email = 'agent.lan@realestate.demo'
WHERE lead.code = 'LEAD-DEMO-001'
  AND NOT EXISTS (
      SELECT 1 FROM ai_lead_scores s WHERE s.lead_id = lead.id AND s.reason LIKE 'Customer has verified budget%'
  );

INSERT INTO ai_conversations (created_by, title, status, last_message_at)
SELECT u.id, 'Demo assistant - buyer follow-up', 'OPEN', CURRENT_TIMESTAMP - INTERVAL '10 minutes'
FROM users u
WHERE u.email = 'agent.lan@realestate.demo'
  AND NOT EXISTS (
      SELECT 1 FROM ai_conversations c WHERE c.created_by = u.id AND c.title = 'Demo assistant - buyer follow-up'
  );

INSERT INTO ai_messages (conversation_id, role, content, ai_status, provider, model)
SELECT c.id, v.role, v.content, v.ai_status, v.provider, v.model
FROM ai_conversations c
JOIN users u ON u.id = c.created_by AND u.email = 'agent.lan@realestate.demo'
JOIN (VALUES
    ('USER', 'Draft a follow-up message for the Nguyen Hue apartment buyer.', NULL, NULL, NULL),
    ('ASSISTANT', 'I recommend confirming the second viewing time and attaching the deposit timeline.', 'SUCCESS', 'noop', 'demo-local')
) AS v(role, content, ai_status, provider, model) ON TRUE
WHERE c.title = 'Demo assistant - buyer follow-up'
  AND NOT EXISTS (
      SELECT 1 FROM ai_messages m WHERE m.conversation_id = c.id AND m.content = v.content
  );

INSERT INTO ai_image_analyses (property_image_id, generated_by, result_json, fallback_used, ai_status, provider, model)
SELECT pi.id, u.id, '{"quality":"good","tags":["interior","bright","city-view"],"recommendation":"Use as cover image"}', FALSE, 'SUCCESS', 'noop', 'demo-local'
FROM property_images pi
JOIN properties p ON p.id = pi.property_id AND p.code = 'PROP-DEMO-001'
JOIN users u ON u.email = 'agent.lan@realestate.demo'
WHERE pi.cover_image = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM ai_image_analyses aia WHERE aia.property_image_id = pi.id
  );

INSERT INTO audit_logs (actor_id, action, resource_type, resource_id, old_value_json, new_value_json)
SELECT u.id, v.action, v.resource_type, ref.id, v.old_value_json, v.new_value_json
FROM (VALUES
    ('manager@realestate.demo', 'LISTING_APPROVED', 'LISTING', 'LIST-DEMO-001', '{"status":"PENDING_REVIEW"}', '{"status":"PUBLISHED"}'),
    ('manager@realestate.demo', 'COMMISSION_APPROVED', 'COMMISSION', 'TX-DEMO-001', '{"status":"PENDING"}', '{"status":"APPROVED"}')
) AS v(actor_email, action, resource_type, resource_code, old_value_json, new_value_json)
JOIN users u ON u.email = v.actor_email
LEFT JOIN listings ref_listing ON v.resource_type = 'LISTING' AND ref_listing.code = v.resource_code
LEFT JOIN transactions ref_tx ON v.resource_type = 'COMMISSION' AND ref_tx.code = v.resource_code
CROSS JOIN LATERAL (SELECT COALESCE(ref_listing.id, ref_tx.id) AS id) ref
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs al WHERE al.action = v.action AND al.resource_type = v.resource_type AND al.resource_id = ref.id
);

COMMIT;
