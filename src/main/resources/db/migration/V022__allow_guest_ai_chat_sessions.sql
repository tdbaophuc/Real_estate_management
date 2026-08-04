ALTER TABLE ai_conversations
    ALTER COLUMN created_by DROP NOT NULL;

ALTER TABLE ai_conversations
    ADD COLUMN guest_session_id VARCHAR(100);

CREATE INDEX idx_ai_conversations_guest_session
    ON ai_conversations (guest_session_id, created_at);
