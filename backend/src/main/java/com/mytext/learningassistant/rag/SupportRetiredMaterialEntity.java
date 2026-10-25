package com.mytext.learningassistant.rag;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Keeps replaced support uploads out of published knowledge bases. */
@Entity
@Table(name = "support_retired_material")
public class SupportRetiredMaterialEntity {
    @Id
    @Column(name = "material_id")
    private Long materialId;
    @Enumerated(EnumType.STRING)
    @Column(name = "scenario", nullable = false, length = 20)
    private KnowledgeBaseScenario scenario;
    @Column(name = "retired_at", nullable = false)
    private LocalDateTime retiredAt;

    protected SupportRetiredMaterialEntity() {}

    public SupportRetiredMaterialEntity(Long materialId, KnowledgeBaseScenario scenario) {
        this.materialId = materialId;
        this.scenario = scenario;
        this.retiredAt = LocalDateTime.now();
    }

    public Long getMaterialId() { return materialId; }
    public KnowledgeBaseScenario getScenario() { return scenario; }
    public LocalDateTime getRetiredAt() { return retiredAt; }
}
