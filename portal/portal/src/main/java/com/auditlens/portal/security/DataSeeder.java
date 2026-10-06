package com.auditlens.portal.security;

import com.auditlens.portal.domain.AppUser;
import com.auditlens.portal.domain.Role;
import com.auditlens.portal.repo.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds demo accounts on an empty database. Password for all portal accounts: demo123 */
@Component
public class DataSeeder implements CommandLineRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public DataSeeder(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        String pw = encoder.encode("demo123");
        users.save(new AppUser("Riya Mehta", "riya@auditlens.local", Role.Employee, pw));
        users.save(new AppUser("Aman Joshi", "aman@auditlens.local", Role.Employee, pw));
        users.save(new AppUser("Karan Desai", "karan@auditlens.local", Role.ReviewerCompliance, pw));
        users.save(new AppUser("Infrastructure Admin", "infra@auditlens.local", Role.TechnicalAdmin, null));
    }
}
