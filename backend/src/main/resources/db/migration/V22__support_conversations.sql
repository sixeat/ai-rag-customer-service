ALTER TABLE knowledge_base
    ADD COLUMN scenario VARCHAR(20) NOT NULL DEFAULT 'GENERAL';

ALTER TABLE knowledge_base
    ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE support_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    scenario VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_support_conversation_owner (owner_id, updated_at),
    INDEX idx_support_conversation_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE support_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    sources_json LONGTEXT,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_support_message_conversation FOREIGN KEY (conversation_id) REFERENCES support_conversation(id) ON DELETE CASCADE,
    INDEX idx_support_message_conversation (conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
