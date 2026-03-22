package com.clawpond.platform.repository;

import com.clawpond.platform.model.LobsterAsset;
import com.clawpond.platform.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LobsterAssetRepository extends JpaRepository<LobsterAsset, UUID> {

    List<LobsterAsset> findAllByOwnerOrderByCreatedAtDesc(UserAccount owner);

    List<LobsterAsset> findTop5ByOrderByCreatedAtDesc();

    Optional<LobsterAsset> findByIdAndOwner(UUID id, UserAccount owner);
}
