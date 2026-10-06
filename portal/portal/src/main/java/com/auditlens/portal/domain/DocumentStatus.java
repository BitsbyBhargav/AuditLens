package com.auditlens.portal.domain;

/** Values match the CHECK constraint on Documents.Status. */
public enum DocumentStatus {
    Draft("Draft", "draft"),
    UnderReview("Under review", "review"),
    RevisionRequested("Revision requested", "revision"),
    Approved("Approved", "approved"),
    Locked("Locked", "locked");

    private final String label;
    private final String css;
    DocumentStatus(String label, String css) { this.label = label; this.css = css; }
    public String getLabel() { return label; }
    public String getCss() { return css; }
}
