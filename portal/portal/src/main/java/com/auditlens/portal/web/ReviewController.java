package com.auditlens.portal.web;

import com.auditlens.portal.domain.AppUser;
import com.auditlens.portal.domain.Document;
import com.auditlens.portal.domain.DocumentStatus;
import com.auditlens.portal.service.DocumentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class ReviewController {
    private final DocumentService docs;
    private final CurrentUser current;

    public ReviewController(DocumentService docs, CurrentUser current) {
        this.docs = docs; this.current = current;
    }

    @GetMapping("/review")
    public String queue(Model model) {
        model.addAttribute("nav", "queue");
        model.addAttribute("queue", docs.queue());
        model.addAttribute("lockedCount", docs.count(DocumentStatus.Locked));
        model.addAttribute("revisionCount", docs.count(DocumentStatus.RevisionRequested));
        return "review/queue";
    }

    @GetMapping("/review/{id}")
    public String review(@PathVariable int id, Principal p, Model model) {
        AppUser me = current.of(p);
        Document d = docs.get(id);
        if (d.getStatus() != DocumentStatus.UnderReview) return "redirect:/documents/" + id;
        docs.recordView(me, d);
        model.addAttribute("nav", "queue");
        model.addAttribute("d", d);
        model.addAttribute("clauses", docs.clauses(d));
        model.addAttribute("preview", docs.textPreview(d).orElse(null));
        model.addAttribute("ownDocument", d.getUploadedBy().getId().equals(me.getId()));
        return "review/document";
    }

    @PostMapping("/review/{id}/approve")
    public String approve(@PathVariable int id, @RequestParam(required = false) String notes,
                          Principal p, RedirectAttributes flash) {
        try {
            docs.approveAndLock(current.of(p), id, notes);
            flash.addFlashAttribute("notice", "Approved and locked.");
            return "redirect:/documents/" + id;
        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/review/" + id;
        }
    }

    @PostMapping("/review/{id}/revise")
    public String revise(@PathVariable int id, @RequestParam(required = false) String notes,
                         Principal p, RedirectAttributes flash) {
        try {
            docs.requestRevision(current.of(p), id, notes);
            flash.addFlashAttribute("notice", "Revision requested. The submitter can now upload a new version.");
            return "redirect:/review";
        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/review/" + id;
        }
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("nav", "audit");
        model.addAttribute("entries", docs.recentAudit());
        return "audit/trail";
    }
}
