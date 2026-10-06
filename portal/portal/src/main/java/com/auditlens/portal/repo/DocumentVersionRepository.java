package com.auditlens.portal.repo;

import com.auditlens.portal.domain.Document;
import com.auditlens.portal.domain.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Integer> {
    List<DocumentVersion> findByDocumentOrderByVersionNumberDesc(Document document);
}
