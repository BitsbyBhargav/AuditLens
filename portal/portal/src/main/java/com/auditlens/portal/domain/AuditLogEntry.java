package com.auditlens.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "AuditLog")
public class AuditLogEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "DocumentID")
    private Document document;

    /** Null for automated actions (classifier). Requires UserID to be nullable: see migration 002. */
    @ManyToOne
    @JoinColumn(name = "UserID")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "Action", nullable = false, length = 50)
    private AuditAction action;

    @Column(name = "ActionTimestamp", nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "Notes", length = 500)
    private String notes;

    protected AuditLogEntry() {}

    public AuditLogEntry(Document document, AppUser user, AuditAction action, String notes) {
        this.document = document; this.user = user; this.action = action; this.notes = notes;
    }

    public String getActorName() { return user == null ? "AuditLens system" : user.getName(); }

    public Integer getId() { return id; }
    public Document getDocument() { return document; }
    public AppUser getUser() { return user; }
    public AuditAction getAction() { return action; }
    public Instant getTimestamp() { return timestamp; }
    public String getNotes() { return notes; }
}
