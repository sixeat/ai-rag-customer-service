package com.mytext.learningassistant.rag;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseMaterialRepository extends JpaRepository<KnowledgeBaseMaterialEntity, Long> {
    List<KnowledgeBaseMaterialEntity> findByKnowledgeBaseId(Long knowledgeBaseId);
    Optional<KnowledgeBaseMaterialEntity> findByKnowledgeBaseIdAndMaterialId(Long knowledgeBaseId, Long materialId);
}
