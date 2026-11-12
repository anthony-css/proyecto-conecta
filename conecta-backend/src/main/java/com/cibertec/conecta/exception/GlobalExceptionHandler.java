package com.cibertec.conecta.exception;

import com.cibertec.conecta.dto.comun.ApiError;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;


import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getCodigo(), null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), ex.getCodigo(), null);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> conflicto(ConflictoException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), ex.getCodigo(), null);
    }

@ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> restricciones(ConstraintViolationException ex) {
        String detalle = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage()).sorted().distinct()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Parámetros inválidos", "VALIDACION", detalle);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes",
                "CONFLICTO_DATOS", "Verifique duplicados o referencias en uso");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> version(OptimisticLockingFailureException ex) {
        return error(HttpStatus.CONFLICT, "El registro fue modificado por otra operación",
                "VERSION_CONFLICTO", "Recargue los datos antes de volver a intentar");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> autenticacion(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", "NO_AUTENTICADO", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> acceso(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "No tiene permisos para esta operación", "ACCESO_DENEGADO", null);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ApiError error;
        if (ex instanceof BindException validation) {
            String detalle = validation.getBindingResult().getAllErrors().stream()
                    .map(item -> item instanceof FieldError field
                            ? PropertyNamingStrategies.SnakeCaseStrategy.INSTANCE.translate(field.getField())
                                + ": " + (field.isBindingFailure() ? "Formato inválido" : field.getDefaultMessage())
                            : item.getDefaultMessage())
                    .sorted().distinct().collect(Collectors.joining("; "));
            error = ApiError.of("Datos de entrada inválidos", "VALIDACION", detalle);
        } else if (ex instanceof HandlerMethodValidationException) {
            error = ApiError.of("Parámetros inválidos", "VALIDACION", "Revise los parámetros enviados");
        } else if (ex instanceof HttpMessageNotReadableException) {
            error = ApiError.of("El cuerpo de la petición no es válido", "JSON_INVALIDO",
                    "Revise el formato de fechas, horas y números");
        } else if (ex instanceof TypeMismatchException) {
            error = ApiError.of("Parámetros inválidos", "VALIDACION", "Revise el tipo de los parámetros enviados");
        } else if (ex instanceof MissingServletRequestParameterException missing) {
            error = ApiError.of("Falta un parámetro obligatorio", "PARAMETRO_FALTANTE", missing.getParameterName());
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            error = ApiError.of("Método HTTP no soportado", "METODO_NO_SOPORTADO", null);
        } else if (ex instanceof HttpMediaTypeNotSupportedException) {
            error = ApiError.of("Tipo de contenido no soportado", "TIPO_NO_SOPORTADO", null);
        } else if (status.value() == 404) {
            error = ApiError.of("Recurso no encontrado", "RECURSO_NO_ENCONTRADO", null);
        } else if (status.is5xxServerError()) {
            log.error("Error al procesar la petición", ex);
            error = ApiError.of("Ocurrió un error inesperado", "ERROR_INTERNO", null);
        } else {
            error = ApiError.of("La petición no es válida", "VALIDACION", null);
        }
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.putAll(headers);
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(ex, error, jsonHeaders, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado",
                "ERROR_INTERNO", "Contacte al administrador si el problema persiste");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String mensaje, String codigo, String detalle) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(ApiError.of(mensaje, codigo, detalle));
    }
}

