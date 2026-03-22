package com.clawpond.platform.service;

import com.clawpond.platform.dto.AdminUserResponse;
import com.clawpond.platform.dto.AdminUserUpdateRequest;
import com.clawpond.platform.exception.BadRequestException;
import com.clawpond.platform.exception.ResourceNotFoundException;
import com.clawpond.platform.model.Role;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserAccountRepository userAccountRepository;

    public AdminUserService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userAccountRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 管理员可更新用户角色和启用状态，但不能禁用或降级自己。
     */
    @Transactional
    public AdminUserResponse updateUser(UUID id, AdminUserUpdateRequest request, UserAccount currentAdmin) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        boolean editingSelf = user.getId().equals(currentAdmin.getId());
        if (editingSelf && (!request.enabled() || request.role() != Role.ADMIN)) {
            throw new BadRequestException("不能禁用自己，也不能把自己降级为非管理员");
        }

        user.setRole(request.role());
        user.setEnabled(request.enabled());
        return toResponse(userAccountRepository.save(user));
    }

    private AdminUserResponse toResponse(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}

