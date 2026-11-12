package com.cibertec.conecta.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    @Bean(destroyMethod = "delete")
    @Lazy
    public FirebaseApp firebaseApp(
            @Value("${firebase.service-account-path}") String accountPath,
            @Value("${firebase.project-id}") String projectId) throws IOException {
        if (accountPath.isBlank() || projectId.isBlank()) {
            throw new IllegalStateException("Configure FIREBASE_SERVICE_ACCOUNT y FIREBASE_PROJECT_ID");
        }
        try (InputStream input = Files.newInputStream(Path.of(accountPath))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(input))
                    .setProjectId(projectId)
                    .build();
            return FirebaseApp.initializeApp(options, "conecta-backend");
        }
    }

    @Bean
    @Lazy
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }
}
