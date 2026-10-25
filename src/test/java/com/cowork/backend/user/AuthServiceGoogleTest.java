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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthHelper authHelper;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    private AuthService authService;

    private final GoogleUser googleUser =
            new GoogleUser("google-sub-1", "nueva@gmail.com", "Nueva");

    @Test
    void firstLoginCreatesUserWithoutPassword() {
        when(googleTokenVerifier.verify("valid-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nueva@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.loginWithGoogle(
                new GoogleLoginRequest("valid-token", AccountType.OWNER));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("nueva@gmail.com");
        assertThat(response.user().nombre()).isEqualTo("Nueva");
        assertThat(response.user().accountType()).isEqualTo(AccountType.OWNER);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void existingEmailIsLinkedToGoogleAccount() {
        User existing = User.builder()
                .id(7L)
                .email("nueva@gmail.com")
                .nombre("Nueva")
                .accountType(AccountType.TENANT)
                .passwordHash("hash")
                .build();
        when(googleTokenVerifier.verify("valid-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nueva@gmail.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(jwtService.generateToken(existing)).thenReturn("jwt-token");

        AuthResponse response = authService.loginWithGoogle(
                new GoogleLoginRequest("valid-token", AccountType.OWNER));

        assertThat(existing.getGoogleSub()).isEqualTo("google-sub-1");
        assertThat(response.user().accountType()).isEqualTo(AccountType.TENANT);
        verify(userRepository).save(existing);
    }

    @Test
    void existingGoogleUserDoesNotQueryEmailNorSave() {
        User existing = User.builder()
                .id(7L)
                .email("nueva@gmail.com")
                .nombre("Nueva")
                .accountType(AccountType.TENANT)
                .googleSub("google-sub-1")
                .build();
        when(googleTokenVerifier.verify("valid-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("jwt-token");

        authService.loginWithGoogle(new GoogleLoginRequest("valid-token", AccountType.OWNER));

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void emailLinkedToAnotherGoogleAccountIsConflict() {
        User existing = User.builder()
                .email("nueva@gmail.com")
                .nombre("Nueva")
                .accountType(AccountType.TENANT)
                .googleSub("otro-sub")
                .build();
        when(googleTokenVerifier.verify("valid-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nueva@gmail.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.loginWithGoogle(
                new GoogleLoginRequest("valid-token", AccountType.TENANT)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void passwordLoginIsRejectedForGoogleOnlyUser() {
        User user = User.builder()
                .email("nueva@gmail.com")
                .nombre("Nueva")
                .accountType(AccountType.TENANT)
                .build();
        when(userRepository.findByEmail("nueva@gmail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("nueva@gmail.com", "password123")))
                .isInstanceOf(ForbiddenException.class);
    }
}
