package com.clawpond.platform.service;

import com.clawpond.platform.dto.OpenClawRegistrationRequest;
import com.clawpond.platform.dto.OpenClawResponse;
import com.clawpond.platform.dto.OpenClawUpdateRequest;
import com.clawpond.platform.exception.DuplicateResourceException;
import com.clawpond.platform.exception.ResourceNotFoundException;
import com.clawpond.platform.model.OpenClawInstance;
import com.clawpond.platform.model.Role;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.repository.OpenClawInstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class OpenClawService {

    private final OpenClawInstanceRepository openClawInstanceRepository;
    private final TagService tagService;

    public OpenClawService(OpenClawInstanceRepository openClawInstanceRepository, TagService tagService) {
        this.openClawInstanceRepository = openClawInstanceRepository;
        this.tagService = tagService;
    }

    /**
     * 创建一个新的 OpenClaw 实例，并挂上标签。
     */
    @Transactional
    public OpenClawResponse register(OpenClawRegistrationRequest request, UserAccount owner) {
        String normalizedBaseUrl = normalizeBaseUrl(request.baseUrl());
        String normalizedExternalId = normalizeRequiredText(request.externalId());
        if (openClawInstanceRepository.existsByBaseUrl(normalizedBaseUrl)) {
            throw new DuplicateResourceException("访问地址已被注册");
        }
        if (openClawInstanceRepository.existsByExternalId(normalizedExternalId)) {
            throw new DuplicateResourceException("外部标识已被注册");
        }

        OpenClawInstance instance = new OpenClawInstance();
        instance.setName(normalizeRequiredText(request.name()));
        instance.setBaseUrl(normalizedBaseUrl);
        instance.setExternalId(normalizedExternalId);
        instance.setDescription(normalizeOptionalText(request.description()));
        instance.setApiToken(normalizeOptionalText(request.apiToken()));
        instance.setOwner(owner);
        instance.setTags(tagService.resolveTags(request.tagNames()));
        instance.setActive(true);

        return toResponse(openClawInstanceRepository.save(instance));
    }

    /**
     * 管理员可查看全部实例，普通用户仅查看自己拥有的实例。
     */
    @Transactional(readOnly = true)
    public List<OpenClawResponse> listVisible(UserAccount user) {
        if (user.getRole() == Role.ADMIN) {
            return openClawInstanceRepository.findAll()
                    .stream()
                    .sorted(Comparator.comparing(OpenClawInstance::getCreatedAt).reversed())
                    .map(this::toResponse)
                    .toList();
        }
        return openClawInstanceRepository.findAllByOwnerOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 共享池只展示启用中的实例，并支持按标签做全包含筛选。
     */
    @Transactional(readOnly = true)
    public List<OpenClawResponse> listPool(List<String> tagNames) {
        List<String> requiredTags = tagService.normalizeTagNames(tagNames);
        return openClawInstanceRepository.findAllByActiveTrue()
                .stream()
                .sorted(Comparator.comparing(OpenClawInstance::getCreatedAt).reversed())
                .filter(instance -> matchesAllTags(instance, requiredTags))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OpenClawResponse getVisibleById(UUID id, UserAccount user) {
        return toResponse(findManagedInstance(id, user));
    }

    /**
     * 编辑实例时保留 owner 和创建时间，只更新业务字段。
     */
    @Transactional
    public OpenClawResponse update(UUID id, OpenClawUpdateRequest request, UserAccount user) {
        OpenClawInstance instance = findManagedInstance(id, user);
        String normalizedBaseUrl = normalizeBaseUrl(request.baseUrl());
        String normalizedExternalId = normalizeRequiredText(request.externalId());

        if (openClawInstanceRepository.existsByBaseUrlAndIdNot(normalizedBaseUrl, id)) {
            throw new DuplicateResourceException("访问地址已被注册");
        }
        if (openClawInstanceRepository.existsByExternalIdAndIdNot(normalizedExternalId, id)) {
            throw new DuplicateResourceException("外部标识已被注册");
        }

        instance.setName(normalizeRequiredText(request.name()));
        instance.setBaseUrl(normalizedBaseUrl);
        instance.setExternalId(normalizedExternalId);
        instance.setDescription(normalizeOptionalText(request.description()));
        String normalizedApiToken = normalizeOptionalText(request.apiToken());
        if (normalizedApiToken != null) {
            instance.setApiToken(normalizedApiToken);
        }
        instance.setTags(tagService.resolveTags(request.tagNames()));
        instance.setActive(request.active());

        return toResponse(openClawInstanceRepository.save(instance));
    }

    @Transactional
    public void delete(UUID id, UserAccount user) {
        OpenClawInstance instance = findManagedInstance(id, user);
        openClawInstanceRepository.delete(instance);
    }

    private OpenClawResponse toResponse(OpenClawInstance instance) {
        return new OpenClawResponse(
                instance.getId(),
                instance.getName(),
                instance.getBaseUrl(),
                instance.getExternalId(),
                instance.getDescription(),
                instance.isActive(),
                instance.getOwner().getUsername(),
                tagService.toTagNames(instance.getTags()),
                instance.getCreatedAt()
        );
    }

    /**
     * 统一按照角色范围查找实例，避免控制器层重复写权限逻辑。
     */
    private OpenClawInstance findManagedInstance(UUID id, UserAccount user) {
        if (user.getRole() == Role.ADMIN) {
            return openClawInstanceRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("OpenClaw 实例不存在"));
        }
        return openClawInstanceRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("OpenClaw 实例不存在"));
    }

    private boolean matchesAllTags(OpenClawInstance instance, List<String> requiredTags) {
        List<String> actualTagNames = tagService.toTagNames(instance.getTags());
        return actualTagNames.containsAll(requiredTags);
    }

    /**
     * URL 统一去掉首尾空白和尾部斜杠，降低重复注册概率。
     */
    private String normalizeBaseUrl(String baseUrl) {
        String normalized = normalizeRequiredText(baseUrl);
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
