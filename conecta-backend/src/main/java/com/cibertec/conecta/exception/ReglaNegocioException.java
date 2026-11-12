package com.cibertec.conecta.exception;

public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
