package com.auditlens.portal.repo;

import com.auditlens.portal.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
}
