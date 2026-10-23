CREATE TABLE knowledge_base (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_knowledge_base_owner (owner_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_base_material (
    id BIGINT NOT NULL AUTO_INCREMENT,
    knowledge_base_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_knowledge_base_material UNIQUE (knowledge_base_id, material_id),
    CONSTRAINT fk_knowledge_base_material_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_base_material_material FOREIGN KEY (material_id) REFERENCES learning_material(id) ON DELETE CASCADE,
    INDEX idx_knowledge_base_material_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
