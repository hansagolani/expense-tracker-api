package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.AuthResponse;
import com.portfolio.expensetracker.dto.LoginRequest;
import com.portfolio.expensetracker.dto.RegisterRequest;
import com.portfolio.expensetracker.entity.Role;
import com.portfolio.expensetracker.entity.User;
import com.portfolio.expensetracker.exception.DuplicateResourceException;
import com.portfolio.expensetracker.repository.UserRepository;
import com.portfolio.expensetracker.security.JwtService;
import com.portfolio.expensetracker.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    // Register service for new user
    public AuthResponse register(RegisterRequest request) {
        // Check if the username or email already exists
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Save User if it does not exist
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        // Hash the incoming password before saving
        user.setPassword(passwordEncoder.encode(request.password()));
        // Every new user gets USER role by default
        user.setRoles(Set.of(Role.USER));
        userRepository.save(user);

        // Log the user in after saving by generating a token.
        UserPrincipal principal = new UserPrincipal(user);
        return new AuthResponse(jwtService.generateToken(principal), user.getUsername());
    }

    // Logging service for existing user
    public AuthResponse login(LoginRequest request) {
        // Checks the password against the stored hash
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        // Load the User wrapped in UserPrincipal
        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        // Get the token string and create AuthResponse
        return new AuthResponse(jwtService.generateToken(userDetails), userDetails.getUsername());
    }
}