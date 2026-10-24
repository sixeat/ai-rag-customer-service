package com.mytext.learningassistant.rag;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportContentRepository extends JpaRepository<SupportContentEntity, Long> {
    List<SupportContentEntity> findAllByOrderByUpdatedAtDesc();
    List<SupportContentEntity> findByCategoryInAndPublishedTrue(List<SupportContentCategory> categories);
    List<SupportContentEntity> findByMaterialIdIn(Collection<Long> materialIds);
}
