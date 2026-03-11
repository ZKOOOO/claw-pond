package com.clawpond.platform.service;

import com.clawpond.platform.exception.BadRequestException;
import com.clawpond.platform.exception.ResourceNotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LobsterStorageService {

    private final Path storageRoot = Paths.get("data", "uploads", "lobsters");

    public LobsterStorageService() throws IOException {
        Files.createDirectories(storageRoot);
    }

    /**
     * 保存上传文件，并返回落盘后的元数据。
     */
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("上传文件不能为空");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "lobster.bin" : file.getOriginalFilename()
        );
        String storedFilename = UUID.randomUUID() + "-" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = storageRoot.resolve(storedFilename).normalize();

        try {
            file.transferTo(target);
        } catch (IOException exception) {
            throw new BadRequestException("保存上传文件失败");
        }

        return new StoredFile(
                originalFilename,
                storedFilename,
                file.getContentType(),
                file.getSize()
        );
    }

    public Resource loadAsResource(String storedFilename) {
        Path target = storageRoot.resolve(storedFilename).normalize();
        if (!Files.exists(target)) {
            throw new ResourceNotFoundException("上传文件不存在");
        }
        return new FileSystemResource(target);
    }

    public record StoredFile(
            String originalFilename,
            String storedFilename,
            String mediaType,
            long fileSize
    ) {
    }
}

