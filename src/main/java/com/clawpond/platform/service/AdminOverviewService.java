package com.clawpond.platform.service;

import com.clawpond.platform.dto.AdminOverviewResponse;
import com.clawpond.platform.dto.RecentLobsterResponse;
import com.clawpond.platform.dto.RecentOpenClawResponse;
import com.clawpond.platform.dto.RecentUserResponse;
import com.clawpond.platform.dto.RecentWorkJobResponse;
import com.clawpond.platform.repository.LobsterAssetRepository;
import com.clawpond.platform.repository.OpenClawInstanceRepository;
import com.clawpond.platform.repository.TagRepository;
import com.clawpond.platform.repository.UserAccountRepository;
import com.clawpond.platform.repository.WorkJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOverviewService {

    private final UserAccountRepository userAccountRepository;
    private final OpenClawInstanceRepository openClawInstanceRepository;
    private final WorkJobRepository workJobRepository;
    private final LobsterAssetRepository lobsterAssetRepository;
    private final TagRepository tagRepository;
    private final TagService tagService;

    public AdminOverviewService(
            UserAccountRepository userAccountRepository,
            OpenClawInstanceRepository openClawInstanceRepository,
            WorkJobRepository workJobRepository,
            LobsterAssetRepository lobsterAssetRepository,
            TagRepository tagRepository,
            TagService tagService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.openClawInstanceRepository = openClawInstanceRepository;
        this.workJobRepository = workJobRepository;
        this.lobsterAssetRepository = lobsterAssetRepository;
        this.tagRepository = tagRepository;
        this.tagService = tagService;
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse getOverview() {
        return new AdminOverviewResponse(
                userAccountRepository.count(),
                openClawInstanceRepository.count(),
                openClawInstanceRepository.countByActiveTrue(),
                workJobRepository.count(),
                lobsterAssetRepository.count(),
                tagRepository.count(),
                userAccountRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(user -> new RecentUserResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getRole(),
                                user.getCreatedAt()
                        ))
                        .toList(),
                openClawInstanceRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(item -> new RecentOpenClawResponse(
                                item.getId(),
                                item.getName(),
                                item.getOwner().getUsername(),
                                item.isActive(),
                                tagService.toTagNames(item.getTags()),
                                item.getCreatedAt()
                        ))
                        .toList(),
                workJobRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(job -> new RecentWorkJobResponse(
                                job.getId(),
                                job.getTitle(),
                                job.getStatus(),
                                job.getRequester().getUsername(),
                                job.getOpenClawInstance().getName(),
                                job.getLobsterAsset() == null ? null : job.getLobsterAsset().getName(),
                                job.getCreatedAt()
                        ))
                        .toList(),
                lobsterAssetRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(asset -> new RecentLobsterResponse(
                                asset.getId(),
                                asset.getName(),
                                asset.getOwner().getUsername(),
                                tagService.toTagNames(asset.getTags()),
                                asset.getCreatedAt()
                        ))
                        .toList()
        );
    }
}

