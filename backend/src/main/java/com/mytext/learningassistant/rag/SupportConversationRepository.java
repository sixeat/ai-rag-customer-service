package com.mytext.learningassistant.rag;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportConversationRepository extends JpaRepository<SupportConversationEntity, Long> {
    List<SupportConversationEntity> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
    List<SupportConversationEntity> findByStatusOrderByUpdatedAtAsc(SupportStatus status);
    List<SupportConversationEntity> findByStatusInOrderByUpdatedAtAsc(List<SupportStatus> statuses);
    Optional<SupportConversationEntity> findByIdAndOwnerId(Long id, Long ownerId);
}
