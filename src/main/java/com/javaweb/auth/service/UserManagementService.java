package com.javaweb.auth.service;

import com.javaweb.audit.AuditActions;
import com.javaweb.audit.service.AuditLogService;
import com.javaweb.auth.dto.AssignUserRolesRequest;
import com.javaweb.auth.dto.CreateUserRequest;
import com.javaweb.auth.dto.UpdateUserStatusRequest;
import com.javaweb.auth.dto.UserManagementResponse;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.DuplicateResourceException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserManagementService {
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "email", "fullName", "status", "createdAt", "lastLoginAt");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UserManagementResponse createUser(
            CreateUserRequest request,
            AuthUserPrincipal actor
    ) {
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("Phone is already registered");
        }

        Map<RoleCode, Role> rolesByCode = loadRoles(request.roles());
        Set<Role> roles = request.roles().stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(rolesByCode::get)
                .collect(Collectors.toSet());

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName()
        );
        user.setPhone(phone);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.replaceRoles(roles);

        try {
            User saved = userRepository.saveAndFlush(user);
            auditLogService.record(
                    actor,
                    AuditActions.USER_CREATED,
                    AuditActions.USER,
                    saved.getId(),
                    null,
                    Map.of(
                            "email", saved.getEmail(),
                            "roles", roleNames(saved)
                    )
            );
            return UserManagementResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("Email or phone is already registered");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<UserManagementResponse> listUsers(
            int page,
            int size,
            String sortBy,
            Sort.Direction direction
    ) {
        String safeSortBy = requireAllowedSortField(sortBy);
        Page<User> userPage = userRepository.findAll(
                PageRequest.of(page, size, Sort.by(direction, safeSortBy))
        );
        List<Long> ids = userPage.getContent().stream()
                .map(User::getId)
                .toList();
        Map<Long, User> usersById = ids.isEmpty()
                ? Map.of()
                : userRepository.findAllWithRolesByIdIn(ids).stream()
                        .collect(Collectors.toMap(
                                User::getId,
                                user -> user,
                                (left, right) -> left,
                                LinkedHashMap::new
                        ));
        List<UserManagementResponse> content = ids.stream()
                .map(usersById::get)
                .map(UserManagementResponse::from)
                .toList();

        return PageResponse.from(userPage, content);
    }

    @Transactional(readOnly = true)
    public UserManagementResponse getUser(Long userId) {
        return UserManagementResponse.from(requireUser(userId));
    }

    @Transactional
    public UserManagementResponse updateStatus(
            Long userId,
            UpdateUserStatusRequest request,
            AuthUserPrincipal actor
    ) {
        User user = requireUser(userId);
        requireManagerCanModify(user, actor);
        UserStatus previousStatus = user.getStatus();
        if (previousStatus == request.status()) {
            return UserManagementResponse.from(user);
        }
        user.setStatus(request.status());
        if (request.status() != UserStatus.LOCKED) {
            user.setLockedUntil(null);
        }
        auditLogService.record(
                actor,
                AuditActions.USER_STATUS_CHANGED,
                AuditActions.USER,
                user.getId(),
                Map.of("status", previousStatus.name()),
                Map.of("status", request.status().name())
        );
        return UserManagementResponse.from(user);
    }

    @Transactional
    public UserManagementResponse assignRoles(
            Long userId,
            AssignUserRolesRequest request,
            AuthUserPrincipal actor
    ) {
        User user = requireUser(userId);
        requireManagerCanModify(user, actor);
        List<String> previousRoles = roleNames(user);
        List<String> requestedRoles = request.roles().stream()
                .map(Enum::name)
                .sorted()
                .toList();
        if (previousRoles.equals(requestedRoles)) {
            return UserManagementResponse.from(user);
        }
        if (!isAdmin(actor) && request.roles().contains(RoleCode.ADMIN)) {
            throw new BusinessException("Managers cannot assign the ADMIN role");
        }

        Map<RoleCode, Role> rolesByCode = loadRoles(request.roles());

        Set<Role> roles = request.roles().stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(rolesByCode::get)
                .collect(Collectors.toSet());
        user.replaceRoles(roles);
        auditLogService.record(
                actor,
                AuditActions.USER_ROLES_CHANGED,
                AuditActions.USER,
                user.getId(),
                Map.of("roles", previousRoles),
                Map.of("roles", roleNames(user))
        );
        return UserManagementResponse.from(user);
    }

    private List<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getCode)
                .map(Enum::name)
                .sorted()
                .toList();
    }

    private User requireUser(Long userId) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String requireAllowedSortField(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessException("Unsupported user sort field");
        }
        return sortBy;
    }

    private Map<RoleCode, Role> loadRoles(Set<RoleCode> roleCodes) {
        Map<RoleCode, Role> rolesByCode = roleRepository.findAll().stream()
                .filter(role -> roleCodes.contains(role.getCode()))
                .collect(Collectors.toMap(Role::getCode, role -> role));
        if (rolesByCode.size() != roleCodes.size()) {
            throw new ResourceNotFoundException("One or more roles were not found");
        }
        return rolesByCode;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }

    private void requireManagerCanModify(User target, AuthUserPrincipal actor) {
        boolean targetIsAdmin = target.getRoles().stream()
                .anyMatch(role -> role.getCode() == RoleCode.ADMIN);
        if (!isAdmin(actor) && targetIsAdmin) {
            throw new BusinessException("Managers cannot modify administrator accounts");
        }
    }

    private boolean isAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name());
    }
}
