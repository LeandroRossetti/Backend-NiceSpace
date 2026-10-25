package com.cowork.backend.security;

import com.cowork.backend.common.ForbiddenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleIdTokenVerifierService implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierService(@Value("${app.google.client-id:}") String clientId) {
        if (clientId == null || clientId.isBlank()) {
            this.verifier = null;
        } else {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();
        }
    }

    @Override
    public GoogleUser verify(String idToken) {
        if (verifier == null) {
            throw new ForbiddenException("El inicio de sesion con Google no esta configurado");
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new ForbiddenException("Token de Google invalido");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new ForbiddenException("El email de Google no esta verificado");
            }
            String subject = payload.getSubject();
            String email = payload.getEmail();
            if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
                throw new ForbiddenException("Token de Google invalido");
            }
            String name = (String) payload.get("name");
            if (name == null || name.isBlank()) {
                name = email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length());
            }
            return new GoogleUser(subject, email, name);
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            throw new ForbiddenException("No se pudo validar el token de Google");
        }
    }
}
