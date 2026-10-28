package com.example.coffeeloyalty.dto;

import jakarta.validation.constraints.NotNull;

public class RedemptionForm {

    @NotNull(message = "Seleccione un cliente.")
    private Long customerId;

    @NotNull(message = "Seleccione una recompensa.")
    private Long rewardId;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getRewardId() {
        return rewardId;
    }

    public void setRewardId(Long rewardId) {
        this.rewardId = rewardId;
    }
}
