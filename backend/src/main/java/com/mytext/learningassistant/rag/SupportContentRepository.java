package com.mytext.learningassistant.rag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportContentRepository extends JpaRepository<SupportContentEntity, Long> {
    List<SupportContentEntity> findAllByOrderByUpdatedAtDesc();
    List<SupportContentEntity> findByCategoryInAndPublishedTrue(List<SupportContentCategory> categories);
}
