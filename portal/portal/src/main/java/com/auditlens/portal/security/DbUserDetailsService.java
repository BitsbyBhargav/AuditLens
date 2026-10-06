package com.auditlens.portal.security;

import com.auditlens.portal.domain.AppUser;
import com.auditlens.portal.domain.Role;
import com.auditlens.portal.repo.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;

    public DbUserDetailsService(AppUserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) {
        AppUser u = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No such user"));
        // Technical admins never sign in to the application; they work in AWS IAM.
        boolean canSignIn = u.getRole() != Role.TechnicalAdmin && u.getPasswordHash() != null;
        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash() == null ? "!" : u.getPasswordHash())
                .roles(u.getRole().name())
                .disabled(!canSignIn)
                .build();
    }
}
