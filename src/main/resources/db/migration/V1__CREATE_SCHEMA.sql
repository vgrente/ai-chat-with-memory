CREATE TABLE IF NOT EXISTS CHAT_MEMORY (
    conversation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(256),
    description VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    conversation_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp" TIMESTAMP NOT NULL,
    sequence_id BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS spring_ai_chat_memory_conversation_id_timestamp_idx
    ON spring_ai_chat_memory(conversation_id, "timestamp");

CREATE INDEX IF NOT EXISTS spring_ai_chat_memory_conversation_id_sequence_id_idx
    ON spring_ai_chat_memory(conversation_id, sequence_id);