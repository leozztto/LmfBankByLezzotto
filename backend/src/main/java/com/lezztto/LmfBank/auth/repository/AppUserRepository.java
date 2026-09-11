package com.lezztto.LmfBank.auth.repository;

import com.lezztto.LmfBank.auth.domain.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByAccountId(Long accountId);
}
