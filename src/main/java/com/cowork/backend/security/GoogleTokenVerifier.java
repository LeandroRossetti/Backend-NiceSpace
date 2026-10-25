package com.cowork.backend.security;

/**
 * Verifies a Google ID token and exposes the trusted user data it contains.
 * Abstracted as an interface so tests can provide a fake implementation.
 */
public interface GoogleTokenVerifier {

    GoogleUser verify(String idToken);

    record GoogleUser(String subject, String email, String name) {
    }
}
