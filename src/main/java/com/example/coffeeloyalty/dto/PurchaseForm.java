package com.example.coffeeloyalty.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public class PurchaseForm {

    @NotNull(message = "Seleccione un cliente.")
    private Long customerId;

    @NotNull(message = "El monto es obligatorio.")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero.")
    @DecimalMax(value = "1000.00", message = "El monto no puede superar $1,000.00.")
    @Digits(integer = 4, fraction = 2, message = "El monto admite hasta 2 decimales.")
    private BigDecimal amount;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
