package com.auditlens.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "DocumentVersions")
public class DocumentVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VersionID")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "DocumentID")
    private Document document;

    @Column(name = "VersionNumber", nullable = false)
    private int versionNumber;

    @Column(name = "S3Key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "ContentHash", length = 64)
    private String contentHash;

    @Column(name = "FileName", length = 255)
    private String fileName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SubmittedBy")
    private AppUser submittedBy;

    @Column(name = "SubmittedAt")
    private Instant submittedAt = Instant.now();

    protected DocumentVersion() {}

    public DocumentVersion(Document document, int versionNumber, String storageKey, String contentHash,
                           String fileName, AppUser submittedBy) {
        this.document = document; this.versionNumber = versionNumber; this.storageKey = storageKey;
        this.contentHash = contentHash; this.fileName = fileName; this.submittedBy = submittedBy;
    }

    public Integer getId() { return id; }
    public Document getDocument() { return document; }
    public int getVersionNumber() { return versionNumber; }
    public String getStorageKey() { return storageKey; }
    public String getContentHash() { return contentHash; }
    public String getFileName() { return fileName; }
    public AppUser getSubmittedBy() { return submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
}
