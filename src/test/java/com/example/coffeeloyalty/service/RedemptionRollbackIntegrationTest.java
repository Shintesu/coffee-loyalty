package com.example.coffeeloyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.coffeeloyalty.dto.CustomerForm;
import com.example.coffeeloyalty.dto.PurchaseForm;
import com.example.coffeeloyalty.dto.RedemptionForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Redemption;
import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.PurchaseRepository;
import com.example.coffeeloyalty.repository.RedemptionRepository;
import com.example.coffeeloyalty.repository.RewardRepository;

@SpringBootTest
@ActiveProfiles("test")
class RedemptionRollbackIntegrationTest {

    @MockitoSpyBean
    private RedemptionRepository redemptionRepository;

    @MockitoSpyBean
    private CustomerRepository customerRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private RedemptionService redemptionService;

    @BeforeEach
    void cleanBefore() {
        redemptionRepository.deleteAll();
        purchaseRepository.deleteAll();
        customerRepository.deleteAll();
        rewardRepository.deleteAll();
    }

    @AfterEach
    void cleanAfter() {
        redemptionRepository.deleteAll();
        purchaseRepository.deleteAll();
        customerRepository.deleteAll();
        rewardRepository.deleteAll();
    }

    @Test
    void rollsBackBalanceWhenSavingTheRedemptionFails() {
        Customer customer = customerService.create(customerForm("Cliente Reversión", "reversion@example.com"));
        Reward reward = rewardRepository.save(new Reward("Postre del día", "Una porción del postre del día.", 30, true));
        registerPurchase(customer.getId(), "50.00");

        doThrow(new RuntimeException("simulated save failure")).when(redemptionRepository).save(any(Redemption.class));

        assertThatThrownBy(() -> redemptionService.redeem(redemptionForm(customer.getId(), reward.getId())))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated save failure");

        assertThat(customerRepository.findById(customer.getId()).orElseThrow().getPoints()).isEqualTo(50);
        assertThat(redemptionRepository.count()).isZero();
    }

    @Test
    void rollsBackTheRedemptionWhenUpdatingTheBalanceFails() {
        Customer customer = customerService.create(customerForm("Cliente Reversión", "reversion@example.com"));
        Reward reward = rewardRepository.save(new Reward("Postre del día", "Una porción del postre del día.", 30, true));
        registerPurchase(customer.getId(), "50.00");

        doThrow(new RuntimeException("simulated balance failure")).when(customerRepository).save(any(Customer.class));

        assertThatThrownBy(() -> redemptionService.redeem(redemptionForm(customer.getId(), reward.getId())))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated balance failure");

        assertThat(customerRepository.findById(customer.getId()).orElseThrow().getPoints()).isEqualTo(50);
        assertThat(redemptionRepository.count()).isZero();
    }

    private void registerPurchase(Long customerId, String amount) {
        PurchaseForm form = new PurchaseForm();
        form.setCustomerId(customerId);
        form.setAmount(new BigDecimal(amount));
        purchaseService.register(form);
    }

    private RedemptionForm redemptionForm(Long customerId, Long rewardId) {
        RedemptionForm form = new RedemptionForm();
        form.setCustomerId(customerId);
        form.setRewardId(rewardId);
        return form;
    }

    private CustomerForm customerForm(String name, String email) {
        CustomerForm form = new CustomerForm();
        form.setName(name);
        form.setEmail(email);
        return form;
    }
}
