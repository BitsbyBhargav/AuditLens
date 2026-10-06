package com.auditlens.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "Documents")
public class Document {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DocumentID")
    private Integer id;

    @Column(name = "FileName", nullable = false, length = 255)
    private String fileName;

    /** Storage key of the current version. Named S3Key to match the schema; a local path in the demo. */
    @Column(name = "S3Key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "DocumentType", length = 50)
    private String documentType;

    /** Rule-based: number of the 10 risk clauses present (0-10). */
    @Column(name = "RiskScore")
    private Double riskScore;

    /** Label implied by RiskScore using the training buckets (Low 0-2, Medium 3-4, High 5-10). */
    @Column(name = "RuleLabel", length = 10)
    private String ruleLabel;

    /** BERT model prediction. */
    @Column(name = "RiskLabel", length = 10)
    private String riskLabel;

    @Column(name = "RiskConfidence")
    private Double riskConfidence;

    @Column(name = "FlaggedClauses", length = 1000)
    private String flaggedClauses;

    @Lob
    @Column(name = "ClauseSnippets")
    private String clauseSnippets;

    @Column(name = "ClassificationNote", length = 500)
    private String classificationNote;

    /** CUAD or Manual */
    @Column(name = "Source", length = 20)
    private String source;

    /** SHA-256 of the current version, recorded at upload. */
    @Column(name = "ContentHash", length = 64)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 30)
    private DocumentStatus status = DocumentStatus.Draft;

    @Column(name = "VersionNumber", nullable = false)
    private int versionNumber = 1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "UploadedBy")
    private AppUser uploadedBy;

    @Column(name = "UploadedAt")
    private Instant uploadedAt = Instant.now();

    @Column(name = "LockedAt")
    private Instant lockedAt;

    @Column(name = "ReviewerNotes", length = 1000)
    private String reviewerNotes;

    public List<String> getFlaggedClauseList() {
        if (flaggedClauses == null || flaggedClauses.isBlank()) return List.of();
        return Arrays.stream(flaggedClauses.split(",")).map(String::strip).toList();
    }

    public boolean isLocked() { return status == DocumentStatus.Locked; }

    public Integer getId() { return id; }
    public String getFileName() { return fileName; }
    public void setFileName(String v) { fileName = v; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String v) { storageKey = v; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String v) { documentType = v; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double v) { riskScore = v; }
    public String getRuleLabel() { return ruleLabel; }
    public void setRuleLabel(String v) { ruleLabel = v; }
    public String getRiskLabel() { return riskLabel; }
    public void setRiskLabel(String v) { riskLabel = v; }
    public Double getRiskConfidence() { return riskConfidence; }
    public void setRiskConfidence(Double v) { riskConfidence = v; }
    public String getFlaggedClauses() { return flaggedClauses; }
    public void setFlaggedClauses(String v) { flaggedClauses = v; }
    public String getClauseSnippets() { return clauseSnippets; }
    public void setClauseSnippets(String v) { clauseSnippets = v; }
    public String getClassificationNote() { return classificationNote; }
    public void setClassificationNote(String v) { classificationNote = v; }
    public String getSource() { return source; }
    public void setSource(String v) { source = v; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String v) { contentHash = v; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus v) { status = v; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int v) { versionNumber = v; }
    public AppUser getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(AppUser v) { uploadedBy = v; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant v) { lockedAt = v; }
    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String v) { reviewerNotes = v; }
}
