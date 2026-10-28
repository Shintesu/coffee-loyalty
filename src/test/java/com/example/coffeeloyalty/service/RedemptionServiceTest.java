package com.example.coffeeloyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.coffeeloyalty.dto.RedemptionForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Redemption;
import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.exception.CustomerNotFoundException;
import com.example.coffeeloyalty.exception.InactiveRewardException;
import com.example.coffeeloyalty.exception.InsufficientPointsException;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.RedemptionRepository;

@ExtendWith(MockitoExtension.class)
class RedemptionServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-26T15:30:00Z"), ZoneOffset.UTC);

    @Mock
    private RedemptionRepository redemptionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RewardService rewardService;

    private RedemptionService redemptionService;

    @BeforeEach
    void setUp() {
        redemptionService = new RedemptionService(redemptionRepository, customerRepository, rewardService, FIXED_CLOCK);
    }

    @Test
    void redeemsRewardDeductingCostTakenFromReward() {
        Customer customer = customer(1L, "Ana Torres", 50);
        Reward reward = reward(7L, "Postre del día", 30, true);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(rewardService.findById(7L)).thenReturn(reward);
        when(redemptionRepository.save(any(Redemption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Redemption redemption = redemptionService.redeem(form(1L, 7L));

        assertThat(redemption.getCustomer()).isSameAs(customer);
        assertThat(redemption.getReward()).isSameAs(reward);
        assertThat(redemption.getPointsSpent()).isEqualTo(30);
        assertThat(redemption.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 26, 15, 30));
        assertThat(customer.getPoints()).isEqualTo(20);
        verify(redemptionRepository).save(redemption);
        verify(customerRepository).save(customer);
    }

    @Test
    void redeemsRewardWhenBalanceEqualsCostLeavingZero() {
        Customer customer = customer(1L, "Ana Torres", 50);
        Reward reward = reward(7L, "Combo de café", 50, true);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(rewardService.findById(7L)).thenReturn(reward);
        when(redemptionRepository.save(any(Redemption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Redemption redemption = redemptionService.redeem(form(1L, 7L));

        assertThat(redemption.getPointsSpent()).isEqualTo(50);
        assertThat(customer.getPoints()).isZero();
    }

    @Test
    void rejectsRedemptionWhenBalanceIsTooLowWithoutChangingBalance() {
        Customer customer = customer(1L, "Ana Torres", 20);
        Reward reward = reward(7L, "Combo de café", 50, true);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(rewardService.findById(7L)).thenReturn(reward);

        assertThatThrownBy(() -> redemptionService.redeem(form(1L, 7L)))
                .isInstanceOf(InsufficientPointsException.class)
                .hasMessage("Ana Torres tiene 20 puntos y la recompensa Combo de café requiere 50.");

        assertThat(customer.getPoints()).isEqualTo(20);
        verify(redemptionRepository, never()).save(any(Redemption.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void rejectsRedemptionWhenRewardIsInactiveWithoutSavingAnything() {
        Customer customer = customer(1L, "Ana Torres", 50);
        Reward reward = reward(7L, "Combo de café", 50, false);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(rewardService.findById(7L)).thenReturn(reward);

        assertThatThrownBy(() -> redemptionService.redeem(form(1L, 7L)))
                .isInstanceOf(InactiveRewardException.class)
                .hasMessage("La recompensa Combo de café no está disponible.");

        assertThat(customer.getPoints()).isEqualTo(50);
        verify(redemptionRepository, never()).save(any(Redemption.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void inactiveRewardWinsOverInsufficientBalance() {
        Customer customer = customer(1L, "Ana Torres", 20);
        Reward reward = reward(7L, "Combo de café", 50, false);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(rewardService.findById(7L)).thenReturn(reward);

        assertThatThrownBy(() -> redemptionService.redeem(form(1L, 7L)))
                .isInstanceOf(InactiveRewardException.class)
                .isNotInstanceOf(InsufficientPointsException.class);

        verify(redemptionRepository, never()).save(any(Redemption.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void usesFindByIdForUpdateInsteadOfFindById() {
        Customer customer = customer(1L, "Ana Torres", 50);
        Reward reward = reward(7L, "Postre del día", 30, true);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(rewardService.findById(7L)).thenReturn(reward);
        when(redemptionRepository.save(any(Redemption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        redemptionService.redeem(form(1L, 7L));

        verify(customerRepository).findByIdForUpdate(1L);
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void throwsWhenCustomerDoesNotExistAndSavesNothing() {
        when(customerRepository.findByIdForUpdate(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> redemptionService.redeem(form(7L, 1L)))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("No existe el cliente con id 7.");

        verify(redemptionRepository, never()).save(any(Redemption.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    private RedemptionForm form(Long customerId, Long rewardId) {
        RedemptionForm form = new RedemptionForm();
        form.setCustomerId(customerId);
        form.setRewardId(rewardId);
        return form;
    }

    private Customer customer(Long id, String name, int points) {
        Customer customer = new Customer(name, "ana@example.com", LocalDate.of(2026, 1, 10), points);
        customer.setId(id);
        return customer;
    }

    private Reward reward(Long id, String name, int requiredPoints, boolean active) {
        Reward reward = new Reward(name, "Recompensa de prueba", requiredPoints, active);
        reward.setId(id);
        return reward;
    }
}
