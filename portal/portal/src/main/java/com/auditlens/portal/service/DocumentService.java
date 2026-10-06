package com.auditlens.portal.service;

import com.auditlens.portal.domain.*;
import com.auditlens.portal.repo.AuditLogRepository;
import com.auditlens.portal.repo.DocumentRepository;
import com.auditlens.portal.repo.DocumentVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static com.auditlens.portal.domain.DocumentStatus.*;

@Service
public class DocumentService {

    /** The only legal status moves. Locking is reachable only through Approved. */
    private static final Map<DocumentStatus, Set<DocumentStatus>> ALLOWED = Map.of(
            Draft, Set.of(UnderReview),
            UnderReview, Set.of(RevisionRequested, Approved),
            RevisionRequested, Set.of(UnderReview),
            Approved, Set.of(Locked),
            Locked, Set.of());

    static final String NOT_SCORED =
            "Not risk-scored: automatic clause extraction for uploaded files is not built yet. "
            + "The reviewer judges risk directly.";

    public record IntegrityResult(boolean matches, String expected, String actual, boolean readOnly) {}

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final AuditLogRepository auditLog;
    private final AuditService audit;
    private final StorageService storage;
    private final ClassifierClient classifier;
    private final CuadLibrary cuad;
    private final ObjectMapper mapper;

    public DocumentService(DocumentRepository documents, DocumentVersionRepository versions,
                           AuditLogRepository auditLog, AuditService audit, StorageService storage,
                           ClassifierClient classifier, CuadLibrary cuad, ObjectMapper mapper) {
        this.documents = documents; this.versions = versions; this.auditLog = auditLog; this.audit = audit;
        this.storage = storage; this.classifier = classifier; this.cuad = cuad; this.mapper = mapper;
    }

    // ---------- submission ----------

    @Transactional
    public Document importFromCuad(AppUser user, String contractId) throws IOException {
        CuadLibrary.Snippets s = cuad.snippets(contractId);
        byte[] text = cuad.contractText(contractId);
        Document d = create(user, contractId + ".txt", text, "CUAD");
        classify(d, s);
        sendForReview(d, user);
        return d;
    }

    @Transactional
    public Document uploadManual(AppUser user, String fileName, byte[] bytes) {
        Document d = create(user, fileName, bytes, "Manual");
        d.setClassificationNote(NOT_SCORED);
        audit.log(d, null, AuditAction.ClassificationSkipped, NOT_SCORED);
        sendForReview(d, user);
        return d;
    }

    @Transactional
    public void resubmit(AppUser employee, int id, String fileName, byte[] bytes) {
        Document d = get(id);
        requireOwner(d, employee);
        if (d.getStatus() != RevisionRequested) {
            throw new IllegalStateException("Only documents with a requested revision can be resubmitted.");
        }
        int v = d.getVersionNumber() + 1;
        String key = storage.store(d.getId(), v, fileName, bytes);
        String hash = Hashing.sha256(bytes);
        versions.save(new DocumentVersion(d, v, key, hash, fileName, employee));
        d.setVersionNumber(v);
        d.setStorageKey(key);
        d.setContentHash(hash);
        d.setFileName(fileName);
        d.setRiskLabel(null); d.setRiskConfidence(null); d.setRiskScore(null); d.setRuleLabel(null);
        d.setFlaggedClauses(null); d.setClauseSnippets(null);
        d.setClassificationNote("Version " + v + " was uploaded by hand, so it has no risk score. "
                + "Earlier scores applied to earlier versions only.");
        audit.log(d, employee, AuditAction.Resubmitted, "Version " + v + " stored. SHA-256 " + Hashing.shortForm(hash));
        sendForReview(d, employee);
    }

    // ---------- review decisions ----------

    @Transactional
    public void requestRevision(AppUser reviewer, int id, String notes) {
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("Explain what needs to change before requesting a revision.");
        }
        Document d = get(id);
        requireNotSelf(d, reviewer);
        move(d, RevisionRequested);
        d.setReviewerNotes(notes.strip());
        audit.log(d, reviewer, AuditAction.RevisionRequested, notes.strip());
    }

    @Transactional
    public void approveAndLock(AppUser reviewer, int id, String notes) {
        Document d = get(id);
        requireNotSelf(d, reviewer);
        move(d, Approved);
        String n = notes == null || notes.isBlank() ? null : notes.strip();
        if (n != null) d.setReviewerNotes(n);
        audit.log(d, reviewer, AuditAction.Approved, n);

        // Refuse to seal a file that changed after upload.
        String actual = Hashing.sha256(storage.read(d.getStorageKey()));
        if (!actual.equals(d.getContentHash())) {
            throw new IllegalStateException("The stored file no longer matches the hash recorded at upload, "
                    + "so it was not locked. Investigate before approving.");
        }
        storage.lock(d.getStorageKey());
        move(d, Locked);
        d.setLockedAt(Instant.now());
        audit.log(d, reviewer, AuditAction.Locked, "Version " + d.getVersionNumber()
                + " set read-only in local storage, SHA-256 " + Hashing.shortForm(actual)
                + ". Production applies S3 Object Lock (Compliance mode) at this step.");
    }

    @Transactional
    public IntegrityResult verifyIntegrity(AppUser user, int id) {
        Document d = get(id);
        String actual;
        try {
            actual = Hashing.sha256(storage.read(d.getStorageKey()));
        } catch (RuntimeException e) {
            actual = "file missing";
        }
        boolean matches = actual.equals(d.getContentHash());
        boolean readOnly = storage.isLocked(d.getStorageKey());
        audit.log(d, user, matches ? AuditAction.IntegrityVerified : AuditAction.IntegrityCheckFailed,
                matches ? "Current file matches recorded SHA-256 " + Hashing.shortForm(actual)
                        : "Expected " + Hashing.shortForm(d.getContentHash()) + " but found " + Hashing.shortForm(actual));
        return new IntegrityResult(matches, d.getContentHash(), actual, readOnly);
    }

    @Transactional
    public void recordView(AppUser reviewer, Document d) {
        audit.log(d, reviewer, AuditAction.Viewed, null);
    }

    // ---------- queries ----------

    @Transactional(readOnly = true)
    public Document get(int id) {
        return documents.findById(id).orElseThrow(() -> new NoSuchElementException("Document " + id + " not found"));
    }

    public boolean canView(AppUser user, Document d) {
        return user.getRole() == Role.ReviewerCompliance || d.getUploadedBy().getId().equals(user.getId());
    }

    @Transactional(readOnly = true)
    public List<Document> mine(AppUser user) { return documents.findByUploadedByOrderByUploadedAtDesc(user); }

    @Transactional(readOnly = true)
    public List<Document> queue() { return documents.findByStatusOrderByUploadedAtAsc(UnderReview); }

    @Transactional(readOnly = true)
    public long count(DocumentStatus s) { return documents.countByStatus(s); }

    @Transactional(readOnly = true)
    public List<DocumentVersion> versions(Document d) { return versions.findByDocumentOrderByVersionNumberDesc(d); }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> trail(Document d) { return auditLog.findByDocumentOrderByTimestampAscIdAsc(d); }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> recentAudit() { return auditLog.findTop300ByOrderByTimestampDescIdDesc(); }

    public Map<String, String> clauses(Document d) {
        if (d.getClauseSnippets() == null || d.getClauseSnippets().isBlank()) return Map.of();
        try {
            return mapper.readValue(d.getClauseSnippets(), new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /** Text preview for .txt files only (CUAD contracts). */
    public Optional<String> textPreview(Document d) {
        if (!d.getFileName().toLowerCase(Locale.ROOT).endsWith(".txt")) return Optional.empty();
        try {
            String text = new String(storage.read(d.getStorageKey()), StandardCharsets.UTF_8);
            return Optional.of(text.length() > 40_000 ? text.substring(0, 40_000) + "\n\n[Preview truncated]" : text);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // ---------- internals ----------

    private Document create(AppUser user, String fileName, byte[] bytes, String source) {
        Document d = new Document();
        d.setFileName(fileName);
        d.setSource(source);
        d.setDocumentType("Contract");
        d.setUploadedBy(user);
        d.setStorageKey("pending");
        d = documents.save(d);
        String key = storage.store(d.getId(), 1, fileName, bytes);
        String hash = Hashing.sha256(bytes);
        d.setStorageKey(key);
        d.setContentHash(hash);
        versions.save(new DocumentVersion(d, 1, key, hash, fileName, user));
        audit.log(d, user, AuditAction.Uploaded, "Version 1 stored. SHA-256 " + Hashing.shortForm(hash));
        return d;
    }

    private void classify(Document d, CuadLibrary.Snippets s) {
        Map<String, String> present = new LinkedHashMap<>();
        for (String c : RiskRules.RISK_CLAUSES) {
            String v = s.clauses() == null ? null : s.clauses().get(c);
            if (v != null && !v.isBlank()) present.put(c, v.strip());
        }
        d.setRiskScore((double) present.size());
        d.setRuleLabel(RiskRules.labelFor(present.size()));
        d.setFlaggedClauses(String.join(", ", present.keySet()));
        try {
            d.setClauseSnippets(mapper.writeValueAsString(present));
        } catch (JsonProcessingException e) {
            d.setClauseSnippets(null);
        }
        String rule = present.size() + " of " + RiskRules.RISK_CLAUSES.size()
                + " risk clauses present (rule label " + d.getRuleLabel() + ")";

        if (s.modelInput() == null || s.modelInput().isBlank()) {
            String note = "No model input in the prepared CUAD file, so there is no model prediction. " + rule + ".";
            d.setClassificationNote(note);
            audit.log(d, null, AuditAction.ClassificationSkipped, note);
            return;
        }
        Optional<ClassifierClient.Prediction> p = classifier.classify(s.modelInput());
        if (p.isPresent()) {
            d.setRiskLabel(p.get().riskLabel());
            d.setRiskConfidence(p.get().confidence());
            audit.log(d, null, AuditAction.Classified, "Model predicted " + p.get().riskLabel() + " ("
                    + Math.round(p.get().confidence() * 100) + "% confidence). " + rule + ".");
        } else {
            String note = "The classifier service did not respond, so there is no model prediction. " + rule + ".";
            d.setClassificationNote(note);
            audit.log(d, null, AuditAction.ClassificationSkipped, note);
        }
    }

    private void sendForReview(Document d, AppUser actor) {
        move(d, UnderReview);
        audit.log(d, actor, AuditAction.ReviewRequested, null);
    }

    private static void move(Document d, DocumentStatus to) {
        if (!ALLOWED.getOrDefault(d.getStatus(), Set.of()).contains(to)) {
            throw new IllegalStateException("A document that is " + d.getStatus().getLabel().toLowerCase(Locale.ROOT)
                    + " can't move to " + to.getLabel().toLowerCase(Locale.ROOT) + ".");
        }
        d.setStatus(to);
    }

    private static void requireOwner(Document d, AppUser user) {
        if (!d.getUploadedBy().getId().equals(user.getId())) {
            throw new IllegalStateException("Only the person who submitted this document can resubmit it.");
        }
    }

    private static void requireNotSelf(Document d, AppUser reviewer) {
        if (d.getUploadedBy().getId().equals(reviewer.getId())) {
            throw new IllegalStateException("You can't review a document you submitted.");
        }
    }
}
