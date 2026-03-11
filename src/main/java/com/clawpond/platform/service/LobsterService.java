package com.clawpond.platform.service;

import com.clawpond.platform.dto.LobsterResponse;
import com.clawpond.platform.exception.BadRequestException;
import com.clawpond.platform.exception.ResourceNotFoundException;
import com.clawpond.platform.model.LobsterAsset;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.repository.LobsterAssetRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class LobsterService {

    private final LobsterAssetRepository lobsterAssetRepository;
    private final TagService tagService;
    private final LobsterStorageService lobsterStorageService;

    public LobsterService(
            LobsterAssetRepository lobsterAssetRepository,
            TagService tagService,
            LobsterStorageService lobsterStorageService
    ) {
        this.lobsterAssetRepository = lobsterAssetRepository;
        this.tagService = tagService;
        this.lobsterStorageService = lobsterStorageService;
    }

    /**
     * 上传用户自己的龙虾，保存文件和标签信息。
     */
    @Transactional
    public LobsterResponse upload(
            String name,
            String description,
            List<String> tagNames,
            MultipartFile file,
            UserAccount owner
    ) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("龙虾名称不能为空");
        }

        LobsterStorageService.StoredFile storedFile = lobsterStorageService.store(file);

        LobsterAsset lobsterAsset = new LobsterAsset();
        lobsterAsset.setName(name.trim());
        lobsterAsset.setDescription(normalizeOptionalText(description));
        lobsterAsset.setOriginalFilename(storedFile.originalFilename());
        lobsterAsset.setStoredFilename(storedFile.storedFilename());
        lobsterAsset.setMediaType(storedFile.mediaType());
        lobsterAsset.setFileSize(storedFile.fileSize());
        lobsterAsset.setOwner(owner);
        lobsterAsset.setTags(tagService.resolveTags(tagNames));

        return toResponse(lobsterAssetRepository.save(lobsterAsset));
    }

    @Transactional(readOnly = true)
    public List<LobsterResponse> listMine(UserAccount owner) {
        return lobsterAssetRepository.findAllByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LobsterDownload download(UUID id, UserAccount owner) {
        LobsterAsset lobsterAsset = lobsterAssetRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("龙虾文件不存在"));

        Resource resource = lobsterStorageService.loadAsResource(lobsterAsset.getStoredFilename());
        return new LobsterDownload(
                resource,
                lobsterAsset.getOriginalFilename(),
                lobsterAsset.getMediaType()
        );
    }

    private LobsterResponse toResponse(LobsterAsset lobsterAsset) {
        return new LobsterResponse(
                lobsterAsset.getId(),
                lobsterAsset.getName(),
                lobsterAsset.getDescription(),
                lobsterAsset.getOriginalFilename(),
                "/api/lobsters/" + lobsterAsset.getId() + "/download",
                tagService.toTagNames(lobsterAsset.getTags()),
                lobsterAsset.getCreatedAt()
        );
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record LobsterDownload(
            Resource resource,
            String originalFilename,
            String mediaType
    ) {
    }
}

