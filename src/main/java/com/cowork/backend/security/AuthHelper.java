package com.cowork.backend.security;

import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.user.User;
import com.cowork.backend.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

    private final UserRepository userRepository;

    public AuthHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof String email)) {
            throw new ForbiddenException("No autenticado");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}
