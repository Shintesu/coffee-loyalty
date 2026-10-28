package com.example.coffeeloyalty.exception;

public class InsufficientPointsException extends RuntimeException {

    public InsufficientPointsException(String customerName, Integer points, String rewardName, Integer requiredPoints) {
        super(customerName + " tiene " + points + " puntos y la recompensa " + rewardName
                + " requiere " + requiredPoints + ".");
    }
}
