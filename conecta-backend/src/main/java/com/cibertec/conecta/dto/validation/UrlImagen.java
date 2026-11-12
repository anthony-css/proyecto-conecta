package com.cibertec.conecta.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UrlImagenValidator.class)
public @interface UrlImagen {
    String message() default "La imagen debe tener una URL absoluta HTTP o HTTPS válida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
