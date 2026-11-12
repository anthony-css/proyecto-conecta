package com.cibertec.conecta.dto.evento;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import com.cibertec.conecta.dto.validation.UrlImagen;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class EventoRequest {

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 120, message = "No debe superar {max} caracteres")
    private String titulo;

    @NotBlank(message = "La descripcion es obligatoria")
    private String descripcion;

    @Size(max = 255, message = "La URL de imagen no debe superar 255 caracteres")
    @UrlImagen
    private String imagenUrl;

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl == null || imagenUrl.isBlank() ? null : imagenUrl.trim();
    }

    @NotNull(message = "La categoria es obligatoria")
    @Positive(message = "El identificador de categoria debe ser mayor que cero")
    private Integer idCategoria;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @NotBlank(message = "La ubicacion es obligatoria")
    @Size(max = 150, message = "No debe superar {max} caracteres")
    private String ubicacion;

    @Size(max = 200, message = "No debe superar {max} caracteres")
    private String direccion;

    @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90")
    @Digits(integer = 3, fraction = 7, message = "La latitud admite hasta 7 decimales")
    private BigDecimal latitud;

    @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180")
    @Digits(integer = 3, fraction = 7, message = "La longitud admite hasta 7 decimales")
    private BigDecimal longitud;

    @NotNull(message = "El aforo es obligatorio")
    @Positive(message = "El aforo debe ser mayor que cero")
    private Integer aforo;

    private Boolean destacado;

    @PositiveOrZero(message = "La version no puede ser negativa")
    private Long version;
}
