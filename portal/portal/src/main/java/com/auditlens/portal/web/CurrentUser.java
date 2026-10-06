package com.auditlens.portal.web;

import com.auditlens.portal.domain.AppUser;
import com.auditlens.portal.repo.AppUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class CurrentUser {
    private final AppUserRepository users;

    public CurrentUser(AppUserRepository users) { this.users = users; }

    public AppUser of(Principal principal) {
        if (principal == null) throw new AccessDeniedException("Not signed in");
        return users.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
    }
}
