CREATE TABLE support_content (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(128) NOT NULL,
    body LONGTEXT,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    starts_at DATETIME(6),
    ends_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_support_content_material FOREIGN KEY (material_id) REFERENCES learning_material(id) ON DELETE CASCADE,
    INDEX idx_support_content_category (category, is_published, starts_at, ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
