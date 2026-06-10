package com.javaweb.auth.service;

import com.javaweb.auth.dto.AuthUserResponse;
import com.javaweb.auth.dto.LoginRequest;
import com.javaweb.auth.dto.LoginResponse;
import com.javaweb.auth.dto.RegisterRequest;
import com.javaweb.auth.dto.RegisterResponse;
import com.javaweb.auth.entity.RefreshToken;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.auth.security.JwtService;
import com.javaweb.common.exception.DuplicateResourceException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.exception.UnauthorizedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}
