package com.auditlens.portal.service;

import com.auditlens.portal.domain.AppUser;
import com.auditlens.portal.domain.AuditAction;
import com.auditlens.portal.domain.AuditLogEntry;
import com.auditlens.portal.domain.Document;
import com.auditlens.portal.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

/** Application-level audit log. Entries are only ever inserted, never updated or deleted. */
@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    public void log(Document d, AppUser actor, AuditAction action, String notes) {
        String n = notes == null ? null : (notes.length() > 500 ? notes.substring(0, 497) + "..." : notes);
        repo.save(new AuditLogEntry(d, actor, action, n));
    }
}
