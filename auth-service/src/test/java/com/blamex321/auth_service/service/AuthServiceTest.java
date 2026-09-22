package com.blamex321.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Secret123!");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Secret123!");

        testUser = User.builder()
                .id("user-123")
                .email("test@example.com")
                .password("encoded_secret")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void register_Success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("encoded_secret");

        MessageResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when registering existing email")
    void register_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login and return JWT token with user role")
    void login_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Secret123!", "encoded_secret")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com", "USER")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        verify(jwtUtil).generateToken(eq("test@example.com"), eq("USER"));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when email is not found")
    void login_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match")
    void login_WrongPassword_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Secret123!", "encoded_secret")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }
}
