package com.example.coffeeloyalty.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.coffeeloyalty.dto.CustomerForm;
import com.example.coffeeloyalty.dto.PurchaseForm;
import com.example.coffeeloyalty.dto.RedemptionForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.service.CustomerService;
import com.example.coffeeloyalty.service.PurchaseService;
import com.example.coffeeloyalty.service.RedemptionService;
import com.example.coffeeloyalty.service.RewardService;

@Component
@Profile("demo")
@Order(2)
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final String AMERICANO_REWARD_NAME = "Café americano";

    private final CustomerService customerService;
    private final PurchaseService purchaseService;
    private final RedemptionService redemptionService;
    private final RewardService rewardService;
    private final TransactionTemplate transactionTemplate;

    public DemoDataLoader(CustomerService customerService, PurchaseService purchaseService,
            RedemptionService redemptionService, RewardService rewardService,
            PlatformTransactionManager transactionManager) {
        this.customerService = customerService;
        this.purchaseService = purchaseService;
        this.redemptionService = redemptionService;
        this.rewardService = rewardService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        if (!customerService.findAll().isEmpty()) {
            log.debug("skipping demo data: customers already exist");
            return;
        }

        transactionTemplate.executeWithoutResult(status -> loadDemoData());
        log.info("loaded demo data: 3 customers");
    }

    private void loadDemoData() {
        Customer ana = createCustomer("Ana Torres", "ana.torres@example.com");
        registerPurchase(ana, "42.50");
        registerPurchase(ana, "18.00");
        registerPurchase(ana, "9.99");

        Customer luis = createCustomer("Luis Mendoza", "luis.mendoza@example.com");
        registerPurchase(luis, "4.50");

        createCustomer("María Pérez", "maria.perez@example.com");

        Reward americano = rewardService.findActive().stream()
                .filter(reward -> AMERICANO_REWARD_NAME.equals(reward.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("demo reward not found: " + AMERICANO_REWARD_NAME));
        redemptionService.redeem(redemptionForm(ana.getId(), americano.getId()));
    }

    private Customer createCustomer(String name, String email) {
        CustomerForm form = new CustomerForm();
        form.setName(name);
        form.setEmail(email);
        return customerService.create(form);
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
}
