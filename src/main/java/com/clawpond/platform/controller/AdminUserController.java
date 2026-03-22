package com.clawpond.platform.controller;

import com.clawpond.platform.dto.AdminUserResponse;
import com.clawpond.platform.dto.AdminUserUpdateRequest;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.service.AdminUserService;
import com.clawpond.platform.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuthService authService;

    public AdminUserController(AdminUserService adminUserService, AuthService authService) {
        this.adminUserService = adminUserService;
        this.authService = authService;
    }

    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return adminUserService.listUsers();
    }

    @PutMapping("/{id}")
    public AdminUserResponse updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserAccount currentAdmin = authService.getCurrentUser(userDetails.getUsername());
        return adminUserService.updateUser(id, request, currentAdmin);
    }
}
