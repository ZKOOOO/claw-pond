package com.clawpond.platform.controller;

import com.clawpond.platform.dto.AdminOverviewResponse;
import com.clawpond.platform.service.AdminOverviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminOverviewController {

    private final AdminOverviewService adminOverviewService;

    public AdminOverviewController(AdminOverviewService adminOverviewService) {
        this.adminOverviewService = adminOverviewService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminOverviewResponse overview() {
        return adminOverviewService.getOverview();
    }
}
