package com.clawpond.platform.service;

import com.clawpond.platform.dto.WorkJobCreateRequest;
import com.clawpond.platform.dto.WorkJobResponse;
import com.clawpond.platform.exception.ResourceNotFoundException;
import com.clawpond.platform.model.OpenClawInstance;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.model.WorkJob;
import com.clawpond.platform.model.WorkJobStatus;
import com.clawpond.platform.repository.OpenClawInstanceRepository;
import com.clawpond.platform.repository.WorkJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkJobService {

    private final WorkJobRepository workJobRepository;
    private final OpenClawInstanceRepository openClawInstanceRepository;
    private final TagService tagService;

    public WorkJobService(
            WorkJobRepository workJobRepository,
            OpenClawInstanceRepository openClawInstanceRepository,
            TagService tagService
    ) {
        this.workJobRepository = workJobRepository;
        this.openClawInstanceRepository = openClawInstanceRepository;
        this.tagService = tagService;
    }

    /**
     * 用户从 OpenClaw 池中选定一个实例，并创建一条任务单。
     */
    @Transactional
    public WorkJobResponse create(WorkJobCreateRequest request, UserAccount requester) {
        OpenClawInstance openClawInstance = openClawInstanceRepository.findByIdAndActiveTrue(request.openClawId())
                .orElseThrow(() -> new ResourceNotFoundException("可用的 OpenClaw 实例不存在"));

        WorkJob workJob = new WorkJob();
        workJob.setTitle(request.title().trim());
        workJob.setDescription(normalizeOptionalText(request.description()));
        workJob.setStatus(WorkJobStatus.CREATED);
        workJob.setRequester(requester);
        workJob.setOpenClawInstance(openClawInstance);
        workJob.setDesiredTags(tagService.resolveTags(request.desiredTags()));

        return toResponse(workJobRepository.save(workJob));
    }

    @Transactional(readOnly = true)
    public List<WorkJobResponse> listMine(UserAccount requester) {
        return workJobRepository.findAllByRequesterOrderByCreatedAtDesc(requester)
                .stream()
                .map(this::toResponse)
                .toList();
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
                tagService.toTagNames(workJob.getDesiredTags()),
                workJob.getCreatedAt()
        );
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
