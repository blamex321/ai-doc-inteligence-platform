package com.blamex321.auth_service.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.blamex321.auth_service.dto.AuthResponse;
import com.blamex321.auth_service.dto.LoginRequest;
import com.blamex321.auth_service.dto.MessageResponse;
import com.blamex321.auth_service.dto.RegisterRequest;
import com.blamex321.auth_service.entity.Role;
import com.blamex321.auth_service.entity.User;
import com.blamex321.auth_service.exception.InvalidCredentialsException;
import com.blamex321.auth_service.exception.UserAlreadyExistsException;
import com.blamex321.auth_service.repository.UserRepository;
import com.blamex321.auth_service.utility.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public MessageResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return new MessageResponse("User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String role = user.getRole() != null ? user.getRole().name() : Role.USER.name();
        String token = jwtUtil.generateToken(user.getEmail(), role);

        return new AuthResponse(token);
    }
}