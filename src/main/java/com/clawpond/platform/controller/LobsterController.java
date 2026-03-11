package com.clawpond.platform.controller;

import com.clawpond.platform.dto.LobsterResponse;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.service.AuthService;
import com.clawpond.platform.service.LobsterService;
import com.clawpond.platform.service.TagService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lobsters")
public class LobsterController {

    private final LobsterService lobsterService;
    private final AuthService authService;
    private final TagService tagService;

    public LobsterController(LobsterService lobsterService, AuthService authService, TagService tagService) {
        this.lobsterService = lobsterService;
        this.authService = authService;
        this.tagService = tagService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LobsterResponse upload(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tagText", required = false) String tagText,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserAccount owner = authService.getCurrentUser(userDetails.getUsername());
        return lobsterService.upload(name, description, tagService.parseTagText(tagText), file, owner);
    }

    @GetMapping
    public List<LobsterResponse> listMine(@AuthenticationPrincipal UserDetails userDetails) {
        UserAccount owner = authService.getCurrentUser(userDetails.getUsername());
        return lobsterService.listMine(owner);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UserAccount owner = authService.getCurrentUser(userDetails.getUsername());
        LobsterService.LobsterDownload download = lobsterService.download(id, owner);

        MediaType mediaType;
        try {
            mediaType = download.mediaType() == null || download.mediaType().isBlank()
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(download.mediaType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(download.originalFilename(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(download.resource());
    }
}
