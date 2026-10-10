package com.auditlens.portal.web;

import com.auditlens.portal.domain.AppUser;
import com.auditlens.portal.domain.Document;
import com.auditlens.portal.domain.DocumentStatus;
import com.auditlens.portal.domain.Role;
import com.auditlens.portal.service.DocumentService;
import com.auditlens.portal.service.StorageService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    public record Step(String name, String state) {}

    private final DocumentService docs;
    private final CurrentUser current;
    private final StorageService storage;

    public DocumentController(DocumentService docs, CurrentUser current, StorageService storage) {
        this.docs = docs; this.current = current; this.storage = storage;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable int id, Principal p, Model model) {
        AppUser me = current.of(p);
        Document d = docs.get(id);
        if (!docs.canView(me, d)) throw new AccessDeniedException("Not your document");
        model.addAttribute("nav", me.getRole() == Role.ReviewerCompliance ? "queue" : "mine");
        model.addAttribute("d", d);
        model.addAttribute("steps", lifecycle(d));
        model.addAttribute("versions", docs.versions(d));
        model.addAttribute("trail", docs.trail(d));
        model.addAttribute("clauses", docs.clauses(d));
        model.addAttribute("isOwner", d.getUploadedBy().getId().equals(me.getId()));
        model.addAttribute("isReviewer", me.getRole() == Role.ReviewerCompliance);
        return "documents/detail";
    }

    /**
     * Serves the stored file to its owner or a reviewer. PDFs and text open in the browser;
     * Word files download. The type comes from the extension and nosniff is set by Spring Security,
     * so an uploaded file can never be interpreted as a web page.
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable int id, Principal p) {
        AppUser me = current.of(p);
        Document d = docs.get(id);
        if (!docs.canView(me, d)) throw new AccessDeniedException("Not your document");

        String name = d.getFileName();
        String lower = name.toLowerCase(Locale.ROOT);
        MediaType type;
        boolean inline;
        if (lower.endsWith(".pdf")) {
            type = MediaType.APPLICATION_PDF;
            inline = true;
        } else if (lower.endsWith(".txt")) {
            type = new MediaType("text", "plain", StandardCharsets.UTF_8);
            inline = true;
        } else {
            type = MediaType.APPLICATION_OCTET_STREAM;
            inline = false;
        }
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(name, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(storage.read(d.getStorageKey()));
    }

    @PostMapping("/{id}/verify")
    public String verify(@PathVariable int id, Principal p, RedirectAttributes flash) {
        AppUser me = current.of(p);
        Document d = docs.get(id);
        if (!docs.canView(me, d)) throw new AccessDeniedException("Not your document");
        flash.addFlashAttribute("integrity", docs.verifyIntegrity(me, id));
        return "redirect:/documents/" + id;
    }

    static List<Step> lifecycle(Document d) {
        int reached = switch (d.getStatus()) {
            case Draft -> 0;
            case UnderReview, RevisionRequested -> 2;
            case Approved -> 3;
            case Locked -> 5;
        };
        boolean scored = d.getRiskLabel() != null || d.getRiskScore() != null;
        String[] names = {
                "Uploaded",
                scored ? "Risk scored" : "Not scored",
                d.getStatus() == DocumentStatus.RevisionRequested ? "Revision requested" : "Under review",
                "Approved",
                "Locked"};
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String state = i < reached ? "done" : i == reached ? "current" : "todo";
            if (i == 1 && !scored && reached > 1) state = "skipped";
            if (i == 2 && d.getStatus() == DocumentStatus.RevisionRequested) state = "attention";
            steps.add(new Step(names[i], state));
        }
        return steps;
    }
}