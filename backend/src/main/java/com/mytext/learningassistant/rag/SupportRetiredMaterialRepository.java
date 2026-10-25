package com.mytext.learningassistant.rag;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportRetiredMaterialRepository extends JpaRepository<SupportRetiredMaterialEntity, Long> {
    List<SupportRetiredMaterialEntity> findByMaterialIdIn(Collection<Long> materialIds);
    long countByScenario(KnowledgeBaseScenario scenario);
}
