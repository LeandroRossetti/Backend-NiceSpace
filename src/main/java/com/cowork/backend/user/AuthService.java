package com.cowork.backend.user;

import com.cowork.backend.common.ConflictException;
import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.security.GoogleTokenVerifier;
import com.cowork.backend.security.GoogleTokenVerifier.GoogleUser;
import com.cowork.backend.security.JwtService;
import com.cowork.backend.user.dto.AuthResponse;
import com.cowork.backend.user.dto.GoogleLoginRequest;
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
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthHelper authHelper,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authHelper = authHelper;
        this.googleTokenVerifier = googleTokenVerifier;
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
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ForbiddenException("Credenciales invalidas");
        }
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    /**
     * Google-only login: the ID token is verified server-side and the user is
     * matched by Google subject first, then by email (linking existing accounts).
     * New users are created with the account type chosen on the login screen.
     */
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUser googleUser = googleTokenVerifier.verify(request.idToken());

        User user = userRepository.findByGoogleSub(googleUser.subject())
                .or(() -> userRepository.findByEmail(googleUser.email()))
                .map(existing -> linkGoogleAccount(existing, googleUser))
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(googleUser.email())
                        .nombre(googleUser.name())
                        .accountType(request.accountType())
                        .googleSub(googleUser.subject())
                        .build()));

        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    public UserDto me() {
        return UserDto.from(authHelper.currentUser());
    }

    private User linkGoogleAccount(User user, GoogleUser googleUser) {
        if (user.getGoogleSub() != null && !user.getGoogleSub().equals(googleUser.subject())) {
            throw new ConflictException("La cuenta ya esta vinculada a otro acceso de Google");
        }
        if (user.getGoogleSub() == null) {
            user.setGoogleSub(googleUser.subject());
            return userRepository.save(user);
        }
        return user;
    }
}
