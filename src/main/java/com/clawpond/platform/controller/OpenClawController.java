package com.clawpond.platform.controller;

import com.clawpond.platform.dto.OpenClawRegistrationRequest;
import com.clawpond.platform.dto.OpenClawResponse;
import com.clawpond.platform.dto.OpenClawUpdateRequest;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.service.AuthService;
import com.clawpond.platform.service.OpenClawService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/openclaws")
public class OpenClawController {

    private final OpenClawService openClawService;
    private final AuthService authService;

    public OpenClawController(OpenClawService openClawService, AuthService authService) {
        this.openClawService = openClawService;
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpenClawResponse register(
            @Valid @RequestBody OpenClawRegistrationRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserAccount owner = authService.getCurrentUser(userDetails.getUsername());
        return openClawService.register(request, owner);
    }

    @GetMapping
    public List<OpenClawResponse> list(@AuthenticationPrincipal UserDetails userDetails) {
        UserAccount owner = authService.getCurrentUser(userDetails.getUsername());
        return openClawService.listVisible(owner);
    }

    @GetMapping("/{id}")
    public OpenClawResponse getById(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UserAccount owner = authService.getCurrentUser(userDetails.getUsername());
        return openClawService.getVisibleById(id, owner);
    }

    /**
     * 更新当前用户可管理的 OpenClaw 实例。
     */
    @PutMapping("/{id}")
    public OpenClawResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody OpenClawUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserAccount owner = authService.getCurrentUser(userDetails.getUsername());
        return openClawService.update(id, request, owner);
    }

    /**
     * 删除当前用户可管理的 OpenClaw 实例。
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        UserAccount owner = authService.getCurrentUser(userDetails.getUsername());
        openClawService.delete(id, owner);
    }
}

