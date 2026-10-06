package com.auditlens.portal.service;

import java.util.List;

/** The 10 risk clauses and label buckets used to build the training labels (fine_tune.py). */
public final class RiskRules {
    private RiskRules() {}

    public static final List<String> RISK_CLAUSES = List.of(
            "Anti-Assignment", "Audit Rights", "Change Of Control", "Most Favored Nation",
            "Non-Compete", "Uncapped Liability", "Exclusivity", "Termination For Convenience",
            "Ip Ownership Assignment", "Liquidated Damages");

    /** Matches pd.cut(bins=[-1, 2, 4, 10]): Low 0-2, Medium 3-4, High 5-10. */
    public static String labelFor(int clauseCount) {
        if (clauseCount <= 2) return "Low";
        if (clauseCount <= 4) return "Medium";
        return "High";
    }
}
