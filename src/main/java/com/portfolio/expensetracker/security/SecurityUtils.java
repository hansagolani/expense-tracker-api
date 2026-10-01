package com.portfolio.expensetracker.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    /** Returns the username of the currently authenticated user, from the JWT-populated security context. */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    /** True if the currently authenticated user has the ADMIN role. */
    public static boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
