package com.auditlens.portal.domain;

/** First eight values match the original CHECK constraint; the last three are added by db/migrations/002. */
public enum AuditAction {
    Uploaded("Uploaded"),
    Classified("Classified"),
    ReviewRequested("Sent for review"),
    RevisionRequested("Revision requested"),
    Resubmitted("Resubmitted"),
    Approved("Approved"),
    Locked("Locked"),
    Viewed("Viewed"),
    ClassificationSkipped("Not scored"),
    IntegrityVerified("Integrity verified"),
    IntegrityCheckFailed("Integrity check failed");

    private final String label;
    AuditAction(String label) { this.label = label; }
    public String getLabel() { return label; }
}
