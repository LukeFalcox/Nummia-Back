package com.devfalco.nummia.common.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String resource) {
        return new NotFoundException(resource + " não encontrado(a).");
    }
}
