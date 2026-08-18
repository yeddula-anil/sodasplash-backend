package com.sodasplash.auth_service.service;

import com.sodasplash.auth_service.dto.*;
import com.sodasplash.auth_service.entity.AuthProvider;
import com.sodasplash.auth_service.entity.Role;
import com.sodasplash.auth_service.entity.User;
import com.sodasplash.auth_service.repository.UserRepository;
import com.sodasplash.auth_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // =====================================================
    // REGISTER WITH EMAIL + PASSWORD
    // =====================================================

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                // NEVER take role from frontend
                .role(Role.CUSTOMER)
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return buildAuthResponse(savedUser, token);
    }

    // =====================================================
    // LOGIN WITH EMAIL + PASSWORD
    // =====================================================

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.isActive()) {
            throw new RuntimeException("Your account has been deactivated");
        }

        // Google-only account
        if (user.getPassword() == null) {
            throw new RuntimeException(
                    "This account was created using Google. Please login with Google."
            );
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        return buildAuthResponse(user, token);
    }

    // =====================================================
    // GOOGLE LOGIN / REGISTRATION
    // =====================================================

    public AuthResponse processGoogleUser(
            String email,
            String name,
            String googleId
    ) {
        email = email.trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);

        // NEW GOOGLE USER
        if (user == null) {
            String username = generateUniqueUsername(name, email);

            user = User.builder()
                    .username(username)
                    .email(email)
                    .password(null)
                    .role(Role.CUSTOMER)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(googleId)
                    .build();

            user = userRepository.save(user);
        }

        // EXISTING USER
        else {
            if (!user.isActive()) {
                throw new RuntimeException("Your account has been deactivated");
            }

            /*
             * Existing LOCAL user can also login using Google.
             * Attach the Google provider ID if it is not already present.
             */
            if (user.getProviderId() == null) {
                user.setProviderId(googleId);
                userRepository.save(user);
            }
        }

        String token = jwtService.generateToken(user);

        return buildAuthResponse(user, token);
    }

    // =====================================================
    // GET ALL BD USERS
    // =====================================================

    public List<UserResponse> getAllBDUsers() {
        return userRepository.findByRole(Role.BD)
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    public List<UserResponse> getAllStaff() {
        return userRepository.findByRoleIn(List.of(Role.ADMIN, Role.BD))
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    // =====================================================
    // TOGGLE USER ACTIVE STATUS
    // =====================================================

    public UserResponse toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(!user.isActive());

        User updatedUser = userRepository.save(user);

        return toUserResponse(updatedUser);
    }
    public AuthResponse createStaff(CreateStaffRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        String username = request.getUsername()
                .trim();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException(
                    "Email is already registered"
            );
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException(
                    "Username is already taken"
            );
        }

        if (request.getRole() != Role.ADMIN &&
                request.getRole() != Role.BD) {

            throw new RuntimeException(
                    "Only ADMIN or BD accounts can be created"
            );
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(request.getRole())
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .isActive(true)
                .build();

        User savedUser =
                userRepository.save(user);

        return buildAuthResponse(
                savedUser,
                null
        );
    }

    // =====================================================
    // GENERATE UNIQUE USERNAME
    // =====================================================

    private String generateUniqueUsername(String name, String email) {
        String baseUsername;

        if (name != null && !name.isBlank()) {
            baseUsername = name
                    .replaceAll("\\s+", "")
                    .toLowerCase();
        } else {
            baseUsername = email.substring(0, email.indexOf("@"));
        }

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
    public UserResponse toggleUserRole(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        if (user.getRole() == Role.BD) {
            user.setRole(Role.ADMIN);
        } else if (user.getRole() == Role.ADMIN) {
            user.setRole(Role.BD);
        } else {
            throw new RuntimeException(
                    "Only BD and ADMIN roles can be toggled"
            );
        }

        User updatedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .role(updatedUser.getRole())
                .isActive(updatedUser.isActive())
                .build();
    }

    // =====================================================
    // CREATE RESPONSE
    // =====================================================

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }
}
