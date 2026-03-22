package com.clawpond.platform.repository;

import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.model.WorkJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkJobRepository extends JpaRepository<WorkJob, UUID> {

    List<WorkJob> findAllByRequesterOrderByCreatedAtDesc(UserAccount requester);

    List<WorkJob> findTop5ByOrderByCreatedAtDesc();

    Optional<WorkJob> findByIdAndRequester(UUID id, UserAccount requester);
}

