package com.auditlens.portal.domain;

/** Values match the CHECK constraint on Users.Role. */
public enum Role {
    Employee("Employee"),
    ReviewerCompliance("Compliance reviewer"),
    /** Exists in the schema, but has no portal login: admins work in AWS IAM / console only. */
    TechnicalAdmin("Technical admin");

    private final String label;
    Role(String label) { this.label = label; }
    public String getLabel() { return label; }
}
