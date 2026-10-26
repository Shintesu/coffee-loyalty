package com.example.coffeeloyalty.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Ya existe un cliente con el email " + email + ".");
    }

    public DuplicateEmailException(String email, Throwable cause) {
        super("Ya existe un cliente con el email " + email + ".", cause);
    }
}
