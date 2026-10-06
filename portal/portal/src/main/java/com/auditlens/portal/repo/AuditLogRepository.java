package com.auditlens.portal.repo;

import com.auditlens.portal.domain.AuditLogEntry;
import com.auditlens.portal.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Integer> {
    List<AuditLogEntry> findByDocumentOrderByTimestampAscIdAsc(Document document);
    List<AuditLogEntry> findTop300ByOrderByTimestampDescIdDesc();
}
