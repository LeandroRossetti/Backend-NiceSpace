package com.cowork.backend.user;

import com.cowork.backend.common.ConflictException;
import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.security.JwtService;
import com.cowork.backend.user.dto.AuthResponse;
import com.cowork.backend.user.dto.LoginRequest;
import com.cowork.backend.user.dto.RegisterRequest;
import com.cowork.backend.user.dto.UserDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthHelper authHelper;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthHelper authHelper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authHelper = authHelper;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("El email ya esta registrado");
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nombre(request.nombre())
                .accountType(request.accountType())
                .build();
        user = userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ForbiddenException("Credenciales invalidas"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ForbiddenException("Credenciales invalidas");
        }
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    public UserDto me() {
        return UserDto.from(authHelper.currentUser());
    }
}
