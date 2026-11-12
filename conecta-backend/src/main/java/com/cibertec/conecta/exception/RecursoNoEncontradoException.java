package com.cibertec.conecta.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    private final String codigo;

    public RecursoNoEncontradoException(String mensaje) {
        this(mensaje, "RECURSO_NO_ENCONTRADO");
    }

    public RecursoNoEncontradoException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
