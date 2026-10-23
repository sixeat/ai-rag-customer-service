package com.mytext.learningassistant.rag;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntity, Long> {
    List<KnowledgeBaseEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<KnowledgeBaseEntity> findByScenarioAndPublishedTrue(KnowledgeBaseScenario scenario);
    Optional<KnowledgeBaseEntity> findByIdAndOwnerId(Long id, Long ownerId);
}
