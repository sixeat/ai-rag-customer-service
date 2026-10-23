package com.mytext.learningassistant.rag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity, Long> {
    List<SupportMessageEntity> findByConversationIdOrderByIdAsc(Long conversationId);
}
