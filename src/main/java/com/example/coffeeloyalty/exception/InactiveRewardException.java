package com.example.coffeeloyalty.exception;

public class InactiveRewardException extends RuntimeException {

    public InactiveRewardException(String rewardName) {
        super("La recompensa " + rewardName + " no está disponible.");
    }
}
