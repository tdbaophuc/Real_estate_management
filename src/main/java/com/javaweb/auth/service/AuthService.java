package com.javaweb.auth.service;

import com.javaweb.audit.AuditActions;
import com.javaweb.audit.service.AuditLogService;
import com.javaweb.auth.dto.AuthUserResponse;
import com.javaweb.auth.dto.ChangePasswordRequest;
import com.javaweb.auth.dto.LoginRequest;
import com.javaweb.auth.dto.LoginResponse;
import com.javaweb.auth.dto.ProfileUpdateRequest;
import com.javaweb.auth.dto.RegisterRequest;
import com.javaweb.auth.dto.RegisterResponse;
import com.javaweb.auth.dto.SessionResponse;
import com.javaweb.auth.entity.RefreshToken;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.repository.RefreshTokenRepository;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.auth.security.JwtService;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.DuplicateResourceException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.exception.UnauthorizedException;
import com.javaweb.storage.entity.FileResource;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.service.FileResourceService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FileResourceService fileResourceService;
    private final AuditLogService auditLogService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            RefreshTokenRepository refreshTokenRepository,
            FileResourceService fileResourceService,
            AuditLogService auditLogService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.fileResourceService = fileResourceService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("Phone is already registered");
        }

        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer role not found"));
        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim()
        );
        user.setPhone(phone);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.addRole(customerRole);

        try {
            return RegisterResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("Email or phone is already registered");
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim(),
                            request.password()
                    )
            );
        } catch (AuthenticationException exception) {
            throw new UnauthorizedException("Invalid email or password");
        }

        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        Instant now = Instant.now();
        user.setLastLoginAt(now);

        String accessToken = jwtService.generateAccessToken(principal);
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issue(user, now);
        return LoginResponse.bearer(
                accessToken,
                jwtService.accessTokenExpiresInSeconds(),
                refreshToken.value(),
                refreshToken.expiresInSeconds(),
                AuthUserResponse.from(principal)
        );
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        Instant now = Instant.now();
        RefreshToken currentToken = refreshTokenService.requireActive(rawRefreshToken, now);
        User user = userRepository.findWithRolesById(currentToken.getUser().getId())
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));
        AuthUserPrincipal principal = AuthUserPrincipal.from(user);
        if (!principal.isEnabled() || !principal.isAccountNonLocked()) {
            throw new UnauthorizedException("User account is not active");
        }

        refreshTokenService.revoke(currentToken, now);
        RefreshTokenService.IssuedRefreshToken nextToken =
                refreshTokenService.issue(user, now);
        return LoginResponse.bearer(
                jwtService.generateAccessToken(principal),
                jwtService.accessTokenExpiresInSeconds(),
                nextToken.value(),
                nextToken.expiresInSeconds(),
                AuthUserResponse.from(principal)
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken, Instant.now());
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser(AuthUserPrincipal actor) {
        return AuthUserResponse.from(loadCurrentUser(actor));
    }

    @Transactional
    public AuthUserResponse updateProfile(
            AuthUserPrincipal actor,
            ProfileUpdateRequest request
    ) {
        User user = loadCurrentUser(actor);
        String oldFullName = user.getFullName();
        String oldPhone = user.getPhone();
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        String phone = normalizePhone(request.phone());
        if (phone != null
                && !phone.equals(user.getPhone())
                && userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
            throw new DuplicateResourceException("Phone is already registered");
        }
        user.setPhone(phone);
        User saved = userRepository.saveAndFlush(user);
        auditLogService.record(
                actor,
                AuditActions.USER_PROFILE_UPDATED,
                AuditActions.USER,
                saved.getId(),
                Map.of(
                        "fullName", oldFullName,
                        "phone", oldPhone == null ? "" : oldPhone
                ),
                Map.of(
                        "fullName", saved.getFullName(),
                        "phone", saved.getPhone() == null ? "" : saved.getPhone()
                )
        );
        return AuthUserResponse.from(saved);
    }

    @Transactional
    public void changePassword(AuthUserPrincipal actor, ChangePasswordRequest request) {
        User user = loadCurrentUser(actor);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is invalid");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("New password confirmation does not match");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        int revoked = refreshTokenService.revokeAllActiveForUser(user.getId(), Instant.now());
        auditLogService.record(
                actor,
                AuditActions.PASSWORD_CHANGED,
                AuditActions.USER,
                user.getId(),
                null,
                Map.of("revokedSessions", revoked)
        );
    }

    @Transactional
    public AuthUserResponse uploadAvatar(AuthUserPrincipal actor, MultipartFile file) {
        User user = loadCurrentUser(actor);
        FileResource oldAvatar = user.getAvatarFileResource();
        FileResource newAvatar = fileResourceService.storeImage(
                file,
                FileAccessLevel.PUBLIC,
                actor
        );
        user.setAvatarFileResource(newAvatar);
        User saved = userRepository.saveAndFlush(user);
        if (oldAvatar != null) {
            fileResourceService.delete(oldAvatar);
        }
        auditLogService.record(
                actor,
                AuditActions.USER_AVATAR_UPDATED,
                AuditActions.USER,
                saved.getId(),
                oldAvatar == null ? null : Map.of("fileResourceId", oldAvatar.getId()),
                Map.of("fileResourceId", newAvatar.getId())
        );
        return AuthUserResponse.from(saved);
    }

    @Transactional
    public AuthUserResponse deleteAvatar(AuthUserPrincipal actor) {
        User user = loadCurrentUser(actor);
        FileResource oldAvatar = user.getAvatarFileResource();
        user.setAvatarFileResource(null);
        User saved = userRepository.saveAndFlush(user);
        if (oldAvatar != null) {
            fileResourceService.delete(oldAvatar);
        }
        auditLogService.record(
                actor,
                AuditActions.USER_AVATAR_DELETED,
                AuditActions.USER,
                saved.getId(),
                oldAvatar == null ? null : Map.of("fileResourceId", oldAvatar.getId()),
                null
        );
        return AuthUserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> sessions(AuthUserPrincipal actor) {
        return refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(actor.id())
                .stream()
                .filter(token -> !token.isExpired(Instant.now()))
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional
    public void revokeSession(AuthUserPrincipal actor, Long sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!token.getUser().getId().equals(actor.id())) {
            throw new AccessDeniedException("Access is denied");
        }
        if (!token.isRevoked()) {
            token.setRevokedAt(Instant.now());
        }
        auditLogService.record(
                actor,
                AuditActions.USER_SESSION_REVOKED,
                AuditActions.USER,
                actor.id(),
                null,
                Map.of("sessionId", sessionId)
        );
    }

    @Transactional
    public void revokeSessions(AuthUserPrincipal actor) {
        int revoked = refreshTokenService.revokeAllActiveForUser(actor.id(), Instant.now());
        auditLogService.record(
                actor,
                AuditActions.USER_SESSIONS_REVOKED,
                AuditActions.USER,
                actor.id(),
                null,
                Map.of("revokedSessions", revoked)
        );
    }

    private User loadCurrentUser(AuthUserPrincipal actor) {
        return userRepository.findWithRolesById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}
