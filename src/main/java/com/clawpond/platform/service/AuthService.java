package com.clawpond.platform.service;

import com.clawpond.platform.dto.AuthResponse;
import com.clawpond.platform.dto.LoginRequest;
import com.clawpond.platform.dto.RegisterRequest;
import com.clawpond.platform.dto.UserProfileResponse;
import com.clawpond.platform.exception.DuplicateResourceException;
import com.clawpond.platform.exception.ResourceNotFoundException;
import com.clawpond.platform.model.Role;
import com.clawpond.platform.model.UserAccount;
import com.clawpond.platform.repository.UserAccountRepository;
import com.clawpond.platform.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase();
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already registered");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        UserAccount saved = userAccountRepository.save(user);
        return toAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toAuthResponse(user);
    }

    public UserAccount getCurrentUser(String email) {
        return userAccountRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserProfileResponse toProfile(UserAccount user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    private AuthResponse toAuthResponse(UserAccount user) {
        String token = jwtService.generateToken(user.getEmail(), Map.of(
                "userId", user.getId().toString(),
                "role", user.getRole().name(),
                "username", user.getUsername()
        ));

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

