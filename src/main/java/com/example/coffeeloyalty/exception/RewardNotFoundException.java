package com.example.coffeeloyalty.exception;

public class RewardNotFoundException extends RuntimeException {

    public RewardNotFoundException(Long id) {
        super("No existe la recompensa con id " + id + ".");
    }
}
