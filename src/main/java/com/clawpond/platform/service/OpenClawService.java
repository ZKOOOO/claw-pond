package com.clawpond.platform.service;

import com.clawpond.platform.dto.OpenClawRegistrationRequest;
import com.clawpond.platform.dto.OpenClawResponse;
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

    public OpenClawService(OpenClawInstanceRepository openClawInstanceRepository) {
        this.openClawInstanceRepository = openClawInstanceRepository;
    }

    @Transactional
    public OpenClawResponse register(OpenClawRegistrationRequest request, UserAccount owner) {
        String normalizedBaseUrl = normalizeBaseUrl(request.baseUrl());
        if (openClawInstanceRepository.existsByBaseUrl(normalizedBaseUrl)) {
            throw new DuplicateResourceException("baseUrl already registered");
        }
        if (openClawInstanceRepository.existsByExternalId(request.externalId())) {
            throw new DuplicateResourceException("externalId already registered");
        }

        OpenClawInstance instance = new OpenClawInstance();
        instance.setName(request.name());
        instance.setBaseUrl(normalizedBaseUrl);
        instance.setExternalId(request.externalId());
        instance.setDescription(request.description());
        instance.setApiToken(request.apiToken());
        instance.setOwner(owner);
        instance.setActive(true);

        return toResponse(openClawInstanceRepository.save(instance));
    }

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

    @Transactional(readOnly = true)
    public OpenClawResponse getVisibleById(UUID id, UserAccount user) {
        OpenClawInstance instance;
        if (user.getRole() == Role.ADMIN) {
            instance = openClawInstanceRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("OpenClaw instance not found"));
        } else {
            instance = openClawInstanceRepository.findByIdAndOwner(id, user)
                    .orElseThrow(() -> new ResourceNotFoundException("OpenClaw instance not found"));
        }
        return toResponse(instance);
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
                instance.getCreatedAt()
        );
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}

