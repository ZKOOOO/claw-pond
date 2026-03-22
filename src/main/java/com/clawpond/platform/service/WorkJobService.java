package com.clawpond.platform.service;

import com.clawpond.platform.dto.WorkJobCreateRequest;
import com.clawpond.platform.dto.WorkJobResponse;
import com.clawpond.platform.dto.WorkJobStatusUpdateRequest;
import com.clawpond.platform.exception.BadRequestException;
import com.clawpond.platform.exception.ResourceNotFoundException;
import com.clawpond.platform.model.LobsterAsset;
import com.clawpond.platform.model.OpenClawInstance;
import com.clawpond.platform.model.Role;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.model.WorkJob;
import com.clawpond.platform.model.WorkJobStatus;
import com.clawpond.platform.repository.LobsterAssetRepository;
import com.clawpond.platform.repository.OpenClawInstanceRepository;
import com.clawpond.platform.repository.WorkJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkJobService {

    private final WorkJobRepository workJobRepository;
    private final OpenClawInstanceRepository openClawInstanceRepository;
    private final LobsterAssetRepository lobsterAssetRepository;
    private final TagService tagService;

    public WorkJobService(
            WorkJobRepository workJobRepository,
            OpenClawInstanceRepository openClawInstanceRepository,
            LobsterAssetRepository lobsterAssetRepository,
            TagService tagService
    ) {
        this.workJobRepository = workJobRepository;
        this.openClawInstanceRepository = openClawInstanceRepository;
        this.lobsterAssetRepository = lobsterAssetRepository;
        this.tagService = tagService;
    }

    /**
     * 用户从 OpenClaw 池中选定一个实例，并创建一条任务单。
     */
    @Transactional
    public WorkJobResponse create(WorkJobCreateRequest request, UserAccount requester) {
        OpenClawInstance openClawInstance = openClawInstanceRepository.findByIdAndActiveTrue(request.openClawId())
                .orElseThrow(() -> new ResourceNotFoundException("可用的 OpenClaw 实例不存在"));
        LobsterAsset lobsterAsset = resolveLobsterAsset(request.lobsterAssetId(), requester);
        List<String> desiredTags = tagService.normalizeTagNames(request.desiredTags());

        List<String> openClawTags = tagService.toTagNames(openClawInstance.getTags());
        if (!openClawTags.containsAll(desiredTags)) {
            throw new BadRequestException("所选 OpenClaw 不满足任务期望标签");
        }

        WorkJob workJob = new WorkJob();
        workJob.setTitle(request.title().trim());
        workJob.setDescription(normalizeOptionalText(request.description()));
        workJob.setStatus(WorkJobStatus.CREATED);
        workJob.setRequester(requester);
        workJob.setOpenClawInstance(openClawInstance);
        workJob.setLobsterAsset(lobsterAsset);
        workJob.setDesiredTags(tagService.resolveTags(desiredTags));

        return toResponse(workJobRepository.save(workJob));
    }

    @Transactional(readOnly = true)
    public List<WorkJobResponse> listMine(UserAccount requester) {
        return workJobRepository.findAllByRequesterOrderByCreatedAtDesc(requester)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 更新任务单状态，普通用户只能更新自己的任务，管理员可更新全部任务。
     */
    @Transactional
    public WorkJobResponse updateStatus(UUID id, WorkJobStatusUpdateRequest request, UserAccount actor) {
        WorkJob workJob = findVisibleJob(id, actor);
        validateStatusTransition(workJob.getStatus(), request.status());
        workJob.setStatus(request.status());
        return toResponse(workJobRepository.save(workJob));
    }

    private WorkJobResponse toResponse(WorkJob workJob) {
        return new WorkJobResponse(
                workJob.getId(),
                workJob.getTitle(),
                workJob.getDescription(),
                workJob.getStatus(),
                workJob.getOpenClawInstance().getId(),
                workJob.getOpenClawInstance().getName(),
                tagService.toTagNames(workJob.getOpenClawInstance().getTags()),
                workJob.getLobsterAsset() == null ? null : workJob.getLobsterAsset().getId(),
                workJob.getLobsterAsset() == null ? null : workJob.getLobsterAsset().getName(),
                workJob.getLobsterAsset() == null ? List.of() : tagService.toTagNames(workJob.getLobsterAsset().getTags()),
                tagService.toTagNames(workJob.getDesiredTags()),
                workJob.getCreatedAt()
        );
    }

    private LobsterAsset resolveLobsterAsset(UUID lobsterAssetId, UserAccount requester) {
        if (lobsterAssetId == null) {
            return null;
        }
        return lobsterAssetRepository.findByIdAndOwner(lobsterAssetId, requester)
                .orElseThrow(() -> new ResourceNotFoundException("所选龙虾不存在或不属于当前用户"));
    }

    private WorkJob findVisibleJob(UUID id, UserAccount actor) {
        if (actor.getRole() == Role.ADMIN) {
            return workJobRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("任务单不存在"));
        }
        return workJobRepository.findByIdAndRequester(id, actor)
                .orElseThrow(() -> new ResourceNotFoundException("任务单不存在"));
    }

    private void validateStatusTransition(WorkJobStatus currentStatus, WorkJobStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }
        boolean allowed = switch (currentStatus) {
            case CREATED -> nextStatus == WorkJobStatus.RUNNING || nextStatus == WorkJobStatus.CANCELED;
            case RUNNING -> nextStatus == WorkJobStatus.COMPLETED
                    || nextStatus == WorkJobStatus.FAILED
                    || nextStatus == WorkJobStatus.CANCELED;
            case COMPLETED, FAILED, CANCELED -> false;
        };
        if (!allowed) {
            throw new BadRequestException("当前任务状态不允许这样流转");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

