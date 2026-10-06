package com.auditlens.portal.web;

import com.auditlens.portal.domain.AppUser;
import com.auditlens.portal.repo.AppUserRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/** Makes the signed-in user available to every template as ${me}. */
@ControllerAdvice
public class ViewAdvice {
    private final AppUserRepository users;

    public ViewAdvice(AppUserRepository users) { this.users = users; }

    @ModelAttribute("me")
    public AppUser me(Principal principal) {
        return principal == null ? null : users.findByEmailIgnoreCase(principal.getName()).orElse(null);
    }
}
