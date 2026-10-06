package com.auditlens.portal.web;

import com.auditlens.portal.domain.AppUser;
import com.auditlens.portal.domain.Document;
import com.auditlens.portal.domain.DocumentStatus;
import com.auditlens.portal.service.ClassifierClient;
import com.auditlens.portal.service.CuadLibrary;
import com.auditlens.portal.service.DocumentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    private static final Set<String> ALLOWED = Set.of(".pdf", ".docx", ".txt");

    private final DocumentService docs;
    private final CuadLibrary cuad;
    private final ClassifierClient classifier;
    private final CurrentUser current;

    public EmployeeController(DocumentService docs, CuadLibrary cuad, ClassifierClient classifier, CurrentUser current) {
        this.docs = docs; this.cuad = cuad; this.classifier = classifier; this.current = current;
    }

    @GetMapping
    public String dashboard(Principal p, Model model) {
        List<Document> mine = docs.mine(current.of(p));
        model.addAttribute("nav", "mine");
        model.addAttribute("documents", mine);
        model.addAttribute("needsRevision", mine.stream().filter(d -> d.getStatus() == DocumentStatus.RevisionRequested).count());
        model.addAttribute("inReview", mine.stream().filter(d -> d.getStatus() == DocumentStatus.UnderReview).count());
        model.addAttribute("locked", mine.stream().filter(Document::isLocked).count());
        return "employee/dashboard";
    }

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("nav", "upload");
        model.addAttribute("contracts", cuad.list());
        model.addAttribute("cuadDir", cuad.getDir().toString());
        model.addAttribute("classifierUp", classifier.isAvailable());
        return "employee/upload";
    }

    @PostMapping("/import-cuad")
    public String importCuad(@RequestParam String contractId, Principal p, RedirectAttributes flash) {
        try {
            Document d = docs.importFromCuad(current.of(p), contractId);
            flash.addFlashAttribute("notice", "Submitted for review.");
            return "redirect:/documents/" + d.getId();
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Couldn't import that contract: " + e.getMessage());
            return "redirect:/employee/upload";
        }
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, Principal p, RedirectAttributes flash) {
        String error = validate(file);
        if (error != null) {
            flash.addFlashAttribute("error", error);
            return "redirect:/employee/upload";
        }
        try {
            Document d = docs.uploadManual(current.of(p), file.getOriginalFilename(), file.getBytes());
            flash.addFlashAttribute("notice", "Submitted for review.");
            return "redirect:/documents/" + d.getId();
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/employee/upload";
        }
    }

    @PostMapping("/documents/{id}/resubmit")
    public String resubmit(@PathVariable int id, @RequestParam("file") MultipartFile file,
                           Principal p, RedirectAttributes flash) {
        String error = validate(file);
        if (error != null) {
            flash.addFlashAttribute("error", error);
            return "redirect:/documents/" + id;
        }
        try {
            AppUser me = current.of(p);
            docs.resubmit(me, id, file.getOriginalFilename(), file.getBytes());
            flash.addFlashAttribute("notice", "New version submitted for review.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/documents/" + id;
    }

    private static String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) return "Choose a file to upload.";
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (ALLOWED.stream().noneMatch(name::endsWith)) return "Upload a PDF, Word (.docx) or text (.txt) file.";
        return null;
    }
}
