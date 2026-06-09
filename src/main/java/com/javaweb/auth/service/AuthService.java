package com.javaweb.auth.service;

import com.javaweb.auth.dto.AuthUserResponse;
import com.javaweb.auth.dto.LoginRequest;
import com.javaweb.auth.dto.LoginResponse;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.auth.security.JwtService;
import com.javaweb.common.exception.UnauthorizedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
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
        user.setLastLoginAt(Instant.now());

        String accessToken = jwtService.generateAccessToken(principal);
        return LoginResponse.bearer(
                accessToken,
                jwtService.accessTokenExpiresInSeconds(),
                AuthUserResponse.from(principal)
        );
    }
}
