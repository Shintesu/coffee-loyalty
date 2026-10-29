package com.example.coffeeloyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

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
import com.example.coffeeloyalty.entity.Purchase;
import com.example.coffeeloyalty.entity.Redemption;
import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.exception.InactiveRewardException;
import com.example.coffeeloyalty.exception.InsufficientPointsException;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.PurchaseRepository;
import com.example.coffeeloyalty.repository.RedemptionRepository;
import com.example.coffeeloyalty.repository.RewardRepository;

@SpringBootTest
@ActiveProfiles("test")
class BalanceConsistencyIntegrationTest {

    private final CustomerService customerService;
    private final PurchaseService purchaseService;
    private final RedemptionService redemptionService;
    private final CustomerRepository customerRepository;
    private final PurchaseRepository purchaseRepository;
    private final RedemptionRepository redemptionRepository;
    private final RewardRepository rewardRepository;

    private Reward americano;
    private Reward cappuccino;
    private Reward dessert;
    private Reward combo;
    private Reward retired;

    @Autowired
    BalanceConsistencyIntegrationTest(CustomerService customerService, PurchaseService purchaseService,
            RedemptionService redemptionService, CustomerRepository customerRepository,
            PurchaseRepository purchaseRepository, RedemptionRepository redemptionRepository,
            RewardRepository rewardRepository) {
        this.customerService = customerService;
        this.purchaseService = purchaseService;
        this.redemptionService = redemptionService;
        this.customerRepository = customerRepository;
        this.purchaseRepository = purchaseRepository;
        this.redemptionRepository = redemptionRepository;
        this.rewardRepository = rewardRepository;
    }

    @BeforeEach
    void setUp() {
        clean();
        americano = rewardRepository.save(
                new Reward("Café americano", "Un café americano de la casa.", 10, true));
        cappuccino = rewardRepository.save(
                new Reward("Cappuccino", "Un cappuccino de la casa.", 20, true));
        dessert = rewardRepository.save(
                new Reward("Postre del día", "Una porción del postre del día.", 30, true));
        combo = rewardRepository.save(
                new Reward("Combo de café", "Un café y un postre a elección.", 50, true));
        retired = rewardRepository.save(
                new Reward("Recompensa retirada", "Recompensa que ya no está disponible.", 10, false));
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    @Test
    void keepsBalancesConsistentWithPurchasesAndRejectedRedemptions() {
        Customer customerA = customerService.create(customerForm("Cliente A", "cliente.a@example.com"));
        registerPurchase(customerA, "42.50");
        registerPurchase(customerA, "25.99");
        registerPurchase(customerA, "0.49");
        registerPurchase(customerA, "9.99");

        assertThat(pointsOf(customerA)).isEqualTo(79);

        redeem(customerA, dessert);
        assertThat(pointsOf(customerA)).isEqualTo(49);
        assertThat(redemptionsOf(customerA)).hasSize(1);

        assertThatThrownBy(() -> redeem(customerA, combo))
                .isInstanceOf(InsufficientPointsException.class)
                .hasMessage("Cliente A tiene 49 puntos y la recompensa Combo de café requiere 50.");
        assertThat(pointsOf(customerA)).isEqualTo(49);
        assertThat(redemptionsOf(customerA)).hasSize(1);

        redeem(customerA, cappuccino);
        assertThat(pointsOf(customerA)).isEqualTo(29);
        assertThat(redemptionsOf(customerA)).hasSize(2);

        assertThatThrownBy(() -> redeem(customerA, retired))
                .isInstanceOf(InactiveRewardException.class)
                .hasMessage("La recompensa Recompensa retirada no está disponible.");
        assertThat(pointsOf(customerA)).isEqualTo(29);
        assertThat(redemptionsOf(customerA)).hasSize(2);

        Customer customerB = customerService.create(customerForm("Cliente B", "cliente.b@example.com"));
        registerPurchase(customerB, "4.50");

        assertThat(pointsOf(customerB)).isEqualTo(5);
        assertThat(redemptionsOf(customerB)).isEmpty();

        assertThatThrownBy(() -> redeem(customerB, americano))
                .isInstanceOf(InsufficientPointsException.class)
                .hasMessage("Cliente B tiene 5 puntos y la recompensa Café americano requiere 10.");
        assertThat(pointsOf(customerB)).isEqualTo(5);
        assertThat(redemptionsOf(customerB)).isEmpty();

        assertBalanceIsConsistent(customerA);
        assertBalanceIsConsistent(customerB);

        assertThat(purchasesOf(customerA)).hasSize(4);
        assertThat(redemptionsOf(customerA)).hasSize(2);
        assertThat(pointsOf(customerA)).isEqualTo(29);

        assertThat(purchasesOf(customerB)).hasSize(1);
        assertThat(redemptionsOf(customerB)).isEmpty();
        assertThat(pointsOf(customerB)).isEqualTo(5);
    }

    private void assertBalanceIsConsistent(Customer customer) {
        int earned = purchasesOf(customer).stream()
                .mapToInt(Purchase::getPointsEarned)
                .sum();
        int spent = redemptionsOf(customer).stream()
                .mapToInt(Redemption::getPointsSpent)
                .sum();
        int points = pointsOf(customer);

        assertThat(points).isEqualTo(earned - spent);
        assertThat(points).isGreaterThanOrEqualTo(0);
    }

    private int pointsOf(Customer customer) {
        return customerService.findById(customer.getId()).getPoints();
    }

    private List<Purchase> purchasesOf(Customer customer) {
        return purchaseService.findByCustomerId(customer.getId());
    }

    private List<Redemption> redemptionsOf(Customer customer) {
        return redemptionService.findByCustomerId(customer.getId());
    }

    private void registerPurchase(Customer customer, String amount) {
        PurchaseForm form = new PurchaseForm();
        form.setCustomerId(customer.getId());
        form.setAmount(new BigDecimal(amount));
        purchaseService.register(form);
    }

    private void redeem(Customer customer, Reward reward) {
        RedemptionForm form = new RedemptionForm();
        form.setCustomerId(customer.getId());
        form.setRewardId(reward.getId());
        redemptionService.redeem(form);
    }

    private CustomerForm customerForm(String name, String email) {
        CustomerForm form = new CustomerForm();
        form.setName(name);
        form.setEmail(email);
        return form;
    }

    private void clean() {
        redemptionRepository.deleteAll();
        purchaseRepository.deleteAll();
        customerRepository.deleteAll();
        rewardRepository.deleteAll();
    }
}
