package com.auditlens.portal.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final boolean demoHints;

    public HomeController(@Value("${auditlens.demo-hints:false}") boolean demoHints) {
        this.demoHints = demoHints;
    }

    @GetMapping("/")
    public String home(Authentication auth) {
        boolean reviewer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ReviewerCompliance"));
        return reviewer ? "redirect:/review" : "redirect:/employee";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("demoHints", demoHints);
        return "login";
    }
}
