package com.example.coffeeloyalty.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.coffeeloyalty.dto.CustomerForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Purchase;
import com.example.coffeeloyalty.entity.Redemption;
import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.PurchaseRepository;
import com.example.coffeeloyalty.repository.RedemptionRepository;
import com.example.coffeeloyalty.repository.RewardRepository;
import com.example.coffeeloyalty.service.CustomerService;
import com.example.coffeeloyalty.service.PurchaseService;
import com.example.coffeeloyalty.service.RedemptionService;
import com.example.coffeeloyalty.service.RewardService;

@SpringBootTest
@ActiveProfiles("test")
class DemoDataLoaderIntegrationTest {

    private final CustomerService customerService;
    private final PurchaseService purchaseService;
    private final RedemptionService redemptionService;
    private final RewardService rewardService;
    private final PlatformTransactionManager transactionManager;
    private final CustomerRepository customerRepository;
    private final PurchaseRepository purchaseRepository;
    private final RedemptionRepository redemptionRepository;
    private final RewardRepository rewardRepository;

    @Autowired
    DemoDataLoaderIntegrationTest(CustomerService customerService, PurchaseService purchaseService,
            RedemptionService redemptionService, RewardService rewardService,
            PlatformTransactionManager transactionManager, CustomerRepository customerRepository,
            PurchaseRepository purchaseRepository, RedemptionRepository redemptionRepository,
            RewardRepository rewardRepository) {
        this.customerService = customerService;
        this.purchaseService = purchaseService;
        this.redemptionService = redemptionService;
        this.rewardService = rewardService;
        this.transactionManager = transactionManager;
        this.customerRepository = customerRepository;
        this.purchaseRepository = purchaseRepository;
        this.redemptionRepository = redemptionRepository;
        this.rewardRepository = rewardRepository;
    }

    @BeforeEach
    void cleanBefore() {
        clean();
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    @Test
    void loadsTheThreeDemoCustomersWithTheirBalances() {
        createRewards();

        demoDataLoader().run();

        List<Customer> customers = customerRepository.findAll();
        assertThat(customers).hasSize(3);
        assertThat(customers)
                .extracting(Customer::getEmail, Customer::getPoints)
                .containsExactlyInAnyOrder(
                        tuple("ana.torres@example.com", 61),
                        tuple("luis.mendoza@example.com", 5),
                        tuple("maria.perez@example.com", 0));
        assertThat(customers).noneMatch(customer -> customer.getName().contains("Sebasti"));
        assertThat(purchaseRepository.count()).isEqualTo(4);
        assertThat(redemptionRepository.count()).isEqualTo(1);
    }

    @Test
    void keepsEveryBalanceConsistentWithItsMovements() {
        createRewards();
        demoDataLoader().run();

        List<Customer> customers = customerRepository.findAll();
        assertThat(customers).hasSize(3);
        for (Customer customer : customers) {
            int earned = purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                    .mapToInt(Purchase::getPointsEarned)
                    .sum();
            int spent = redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                    .mapToInt(Redemption::getPointsSpent)
                    .sum();
            assertThat(customer.getPoints()).isEqualTo(earned - spent);
        }
    }

    @Test
    void doesNotDuplicateDemoDataWhenRunTwice() {
        createRewards();
        DemoDataLoader loader = demoDataLoader();

        loader.run();
        loader.run();

        assertThat(customerRepository.count()).isEqualTo(3);
        assertThat(purchaseRepository.count()).isEqualTo(4);
        assertThat(redemptionRepository.count()).isEqualTo(1);
    }

    @Test
    void doesNotLoadDemoDataWhenACustomerAlreadyExists() {
        CustomerForm form = new CustomerForm();
        form.setName("Cliente Existente");
        form.setEmail("existente@example.com");
        customerService.create(form);

        demoDataLoader().run();

        assertThat(customerRepository.findAll())
                .extracting(Customer::getEmail)
                .containsExactly("existente@example.com");
        assertThat(purchaseRepository.count()).isZero();
        assertThat(redemptionRepository.count()).isZero();
    }

    @Test
    void rollsBackDemoDataWhenTheAmericanoRewardIsMissing() {
        createRewardsWithoutAmericano();

        assertThatThrownBy(() -> demoDataLoader().run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("demo reward not found: Café americano");

        assertThat(customerRepository.count()).isZero();
        assertThat(purchaseRepository.count()).isZero();
        assertThat(redemptionRepository.count()).isZero();
    }

    private DemoDataLoader demoDataLoader() {
        return new DemoDataLoader(customerService, purchaseService, redemptionService, rewardService,
                transactionManager);
    }

    private void createRewards() {
        rewardRepository.saveAll(List.of(
                new Reward("Café americano", "Un café americano de la casa.", 10, true),
                new Reward("Cappuccino", "Un cappuccino de la casa.", 20, true),
                new Reward("Postre del día", "Una porción del postre del día.", 30, true),
                new Reward("Combo de café", "Un café y un postre a elección.", 50, true)));
    }

    private void createRewardsWithoutAmericano() {
        rewardRepository.saveAll(List.of(
                new Reward("Cappuccino", "Un cappuccino de la casa.", 20, true),
                new Reward("Postre del día", "Una porción del postre del día.", 30, true),
                new Reward("Combo de café", "Un café y un postre a elección.", 50, true)));
    }

    private void clean() {
        redemptionRepository.deleteAll();
        purchaseRepository.deleteAll();
        customerRepository.deleteAll();
        rewardRepository.deleteAll();
    }
}
