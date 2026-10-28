package com.example.coffeeloyalty.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.coffeeloyalty.dto.CustomerForm;
import com.example.coffeeloyalty.dto.PurchaseForm;
import com.example.coffeeloyalty.dto.RedemptionForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Redemption;
import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.exception.InsufficientPointsException;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.PurchaseRepository;
import com.example.coffeeloyalty.repository.RedemptionRepository;
import com.example.coffeeloyalty.repository.RewardRepository;

@SpringBootTest
@ActiveProfiles("test")
class RedemptionIntegrationTest {

    @Autowired
    private RedemptionRepository redemptionRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

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
    void deletingACustomerDeletesOnlyItsRedemptionsAndKeepsRewards() {
        Reward reward = rewardRepository.save(new Reward("Postre del día", "Una porción del postre del día.", 30, true));
        Customer customer = customerService.create(customerForm("Cliente Cascada", "cascada@example.com"));
        Customer otherCustomer = customerService.create(customerForm("Cliente Conservado", "conservado@example.com"));

        redeem(customer, reward);
        redeem(otherCustomer, reward);

        assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).hasSize(1);
        assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(otherCustomer.getId())).hasSize(1);

        customerService.delete(customer.getId());

        assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).isEmpty();
        assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(otherCustomer.getId())).hasSize(1);
        assertThat(rewardRepository.findAll()).hasSize(1);
    }

    @Test
    void onlyOneOfTwoConcurrentRedemptionsSucceedsWhenTheBalanceOnlyCoversOne() throws Exception {
        Reward reward = rewardRepository.save(new Reward("Postre del día", "Una porción del postre del día.", 30, true));

        for (int i = 0; i < 10; i++) {
            Customer customer = customerService.create(
                    customerForm("Cliente Concurrente " + i, "concurrente" + i + "@example.com"));
            registerPurchase(customer, "50.00");

            int[] result = runConcurrentRedemptions(customer.getId(), reward.getId());

            assertThat(result).containsExactly(1, 1);
            assertThat(customerRepository.findById(customer.getId()).orElseThrow().getPoints()).isEqualTo(20);
            assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).hasSize(1);
        }
    }

    private int[] runConcurrentRedemptions(Long customerId, Long rewardId) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        RedemptionForm first = redemptionForm(customerId, rewardId);
        RedemptionForm second = redemptionForm(customerId, rewardId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Redemption> firstFuture = executor.submit(() -> {
                start.await();
                return redemptionService.redeem(first);
            });
            Future<Redemption> secondFuture = executor.submit(() -> {
                start.await();
                return redemptionService.redeem(second);
            });
            start.countDown();

            int successes = 0;
            int insufficient = 0;
            for (Future<Redemption> future : List.of(firstFuture, secondFuture)) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    successes++;
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof InsufficientPointsException) {
                        insufficient++;
                    } else {
                        throw ex;
                    }
                }
            }
            return new int[]{successes, insufficient};
        } finally {
            executor.shutdownNow();
        }
    }

    private void redeem(Customer customer, Reward reward) {
        registerPurchase(customer, "50.00");
        redemptionService.redeem(redemptionForm(customer.getId(), reward.getId()));
    }

    private void registerPurchase(Customer customer, String amount) {
        PurchaseForm form = new PurchaseForm();
        form.setCustomerId(customer.getId());
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
