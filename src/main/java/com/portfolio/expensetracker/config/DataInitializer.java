package com.portfolio.expensetracker.config;

import com.portfolio.expensetracker.entity.Role;
import com.portfolio.expensetracker.entity.User;
import com.portfolio.expensetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    CommandLineRunner createAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User(
                        adminUsername,
                        adminEmail,
                        passwordEncoder.encode(adminPassword)
                );
                admin.setRoles(Set.of(Role.ADMIN));
                userRepository.save(admin);
            }
        };
    }
}
