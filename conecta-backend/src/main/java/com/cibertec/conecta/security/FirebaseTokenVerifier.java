package com.cibertec.conecta.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class FirebaseTokenVerifier {

    private final ObjectProvider<FirebaseAuth> firebaseAuth;

    public FirebaseTokenVerifier(ObjectProvider<FirebaseAuth> firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    public FirebasePrincipal verificar(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.getObject().verifyIdToken(idToken);
            return new FirebasePrincipal(token.getUid(), token.getEmail(), token.getName(),
                    token.getPicture(), java.util.Map.copyOf(token.getClaims()));
        } catch (FirebaseAuthException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Token de Firebase inválido");
        }
    }
}
