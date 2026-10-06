package com.auditlens.portal.repo;

import com.auditlens.portal.domain.AppUser;
import com.auditlens.portal.domain.Document;
import com.auditlens.portal.domain.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Integer> {
    List<Document> findByUploadedByOrderByUploadedAtDesc(AppUser user);
    List<Document> findByStatusOrderByUploadedAtAsc(DocumentStatus status);
    long countByStatus(DocumentStatus status);
}
