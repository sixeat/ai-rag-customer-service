CREATE TABLE support_retired_material (
    material_id BIGINT NOT NULL,
    scenario VARCHAR(20) NOT NULL,
    retired_at DATETIME(6) NOT NULL,
    PRIMARY KEY (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
