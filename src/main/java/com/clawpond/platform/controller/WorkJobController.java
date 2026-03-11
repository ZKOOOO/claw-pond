package com.clawpond.platform.controller;

import com.clawpond.platform.dto.WorkJobCreateRequest;
import com.clawpond.platform.dto.WorkJobResponse;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.service.AuthService;
import com.clawpond.platform.service.WorkJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work-jobs")
public class WorkJobController {

    private final WorkJobService workJobService;
    private final AuthService authService;

    public WorkJobController(WorkJobService workJobService, AuthService authService) {
        this.workJobService = workJobService;
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkJobResponse create(
            @Valid @RequestBody WorkJobCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserAccount requester = authService.getCurrentUser(userDetails.getUsername());
        return workJobService.create(request, requester);
    }

    @GetMapping
    public List<WorkJobResponse> listMine(@AuthenticationPrincipal UserDetails userDetails) {
        UserAccount requester = authService.getCurrentUser(userDetails.getUsername());
        return workJobService.listMine(requester);
    }
}

