ALTER TABLE follow_up_tasks
    ADD COLUMN reminder_sent_at TIMESTAMP NULL;

ALTER TABLE email_logs
    ADD COLUMN reference_type VARCHAR(100);

ALTER TABLE email_logs
    ADD COLUMN reference_id BIGINT;

ALTER TABLE email_logs
    ADD CONSTRAINT uk_email_logs_reminder_delivery
        UNIQUE (template_id, recipient_email, reference_type, reference_id);

CREATE INDEX idx_follow_up_tasks_reminder_due
    ON follow_up_tasks (reminder_sent_at, status, due_at);

CREATE INDEX idx_email_logs_reference
    ON email_logs (reference_type, reference_id);

INSERT INTO notification_templates (
    code,
    name,
    channel,
    subject_template,
    body_template
)
VALUES (
    'APPOINTMENT_REMINDER_EMAIL',
    'Appointment reminder email',
    'EMAIL',
    'Reminder: {{appointmentTitle}} at {{startTime}}',
    'Hello {{recipientName}}, your appointment "{{appointmentTitle}}" starts at {{startTime}}. Location: {{meetingLocation}}.'
), (
    'FOLLOW_UP_REMINDER_EMAIL',
    'Follow-up task reminder email',
    'EMAIL',
    'Reminder: follow-up task "{{taskTitle}}" is due',
    'Hello {{recipientName}}, the follow-up task "{{taskTitle}}" for lead {{leadName}} is due at {{dueTime}}.'
);
