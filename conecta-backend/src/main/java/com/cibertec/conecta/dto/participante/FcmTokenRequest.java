package com.cibertec.conecta.dto.participante;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FcmTokenRequest(
        @NotBlank(message = "El token FCM es obligatorio") @Size(max = 255, message = "El token FCM no debe superar 255 caracteres") String fcmToken
) {}
