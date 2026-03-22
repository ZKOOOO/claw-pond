package com.clawpond.platform.repository;

import com.clawpond.platform.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<UserAccount> findTop5ByOrderByCreatedAtDesc();

    List<UserAccount> findAllByOrderByCreatedAtDesc();
}

