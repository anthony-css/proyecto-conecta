package com.cibertec.conecta.exception;

public class ConflictoException extends RuntimeException {

    private final String codigo;

    public ConflictoException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
