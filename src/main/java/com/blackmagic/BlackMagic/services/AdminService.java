package com.blackmagic.BlackMagic.services;

import com.blackmagic.BlackMagic.dtos.publicDtos.*;
import com.blackmagic.BlackMagic.dtos.adminDtos.*;
import com.blackmagic.BlackMagic.exception.*;
import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        try {
            // Authenticate using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // Generate JWT token
            String token = jwtService.generateToken(userDetails);

            // Extract roles
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            log.info("User {} logged in successfully", request.getUsername());

            return AdminLoginResponse.builder()
                    .token(token)
                    .username(request.getUsername())
                    .role(roles.isEmpty() ? "USER" : roles.getFirst().replace("ROLE_", ""))
                    .permissions(roles)
                    .build();

        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getUsername(), e);
            throw new AuthenticationException("Invalid username or password");
        }
    }

    @Transactional
    public AdminUser createUser(AdminUserCreateRequest request) {
        // Check if username exists
        if (adminUserRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Username already exists");
        }

        if (adminUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email already exists");
        }

        AdminUser user = AdminUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(AdminUser.UserRole.valueOf(request.getRole()))
                .assignedTables(request.getAssignedTables())
                .permissions(List.of(request.getRole()))
                .active(true)
                .build();

        user = adminUserRepository.save(user);

        log.info("Created admin user: {}", user.getUsername());

        return user;
    }

    public List<AdminUser> getAllUsers() {
        return adminUserRepository.findByActiveTrue();
    }

    public List<AdminUser> getUsersByRole(AdminUser.UserRole role) {
        return adminUserRepository.findByRoleAndActiveTrue(role);
    }

    @Transactional
    public AdminUser updateUser(String userId, AdminUserUpdateRequest request) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getRole() != null) {
            user.setRole(AdminUser.UserRole.valueOf(request.getRole()));
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        if (request.getAssignedTables() != null) {
            user.setAssignedTables(request.getAssignedTables());
        }

        return adminUserRepository.save(user);
    }

    @Transactional
    public void changePassword(String userId, String oldPassword, String newPassword) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserRepository.save(user);

        log.info("Password changed for user: {}", user.getUsername());
    }
}