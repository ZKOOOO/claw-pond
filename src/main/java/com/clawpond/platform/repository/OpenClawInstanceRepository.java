package com.clawpond.platform.repository;

import com.clawpond.platform.model.OpenClawInstance;
import com.clawpond.platform.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpenClawInstanceRepository extends JpaRepository<OpenClawInstance, UUID> {

    boolean existsByBaseUrl(String baseUrl);

    boolean existsByExternalId(String externalId);

    boolean existsByBaseUrlAndIdNot(String baseUrl, UUID id);

    boolean existsByExternalIdAndIdNot(String externalId, UUID id);

    List<OpenClawInstance> findAllByActiveTrue();

    long countByActiveTrue();

    List<OpenClawInstance> findTop5ByOrderByCreatedAtDesc();

    List<OpenClawInstance> findAllByOwnerOrderByCreatedAtDesc(UserAccount owner);

    Optional<OpenClawInstance> findByIdAndOwner(UUID id, UserAccount owner);

    Optional<OpenClawInstance> findByIdAndActiveTrue(UUID id);
}
