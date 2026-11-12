package com.cibertec.conecta.security;

import java.util.Map;

public record FirebasePrincipal(
        String uid,
        String email,
        String name,
        String picture,
        Map<String, Object> claims
) {
    public String nombreParaMostrar() {
        return name != null && !name.isBlank() ? name : email;
    }
}
