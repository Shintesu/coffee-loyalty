package com.example.coffeeloyalty.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.coffeeloyalty.dto.CustomerForm;
import com.example.coffeeloyalty.dto.PurchaseForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.PurchaseRepository;

@SpringBootTest
@ActiveProfiles("test")
class PurchaseCascadeIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PurchaseService purchaseService;

    @BeforeEach
    void cleanBefore() {
        purchaseRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @AfterEach
    void cleanAfter() {
        purchaseRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void deletesPurchasesWhenTheCustomerIsDeleted() {
        Customer customer = customerService.create(customerForm("Cliente Cascada", "cascada@example.com"));
        Customer otherCustomer = customerService.create(customerForm("Cliente Conservado", "conservado@example.com"));

        registerPurchase(customer, "9.99");
        registerPurchase(otherCustomer, "5.00");

        assertThat(purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).hasSize(1);
        assertThat(purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(otherCustomer.getId())).hasSize(1);

        customerService.delete(customer.getId());

        assertThat(purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).isEmpty();
        assertThat(purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(otherCustomer.getId())).hasSize(1);
        assertThat(purchaseRepository.findAll()).hasSize(1);
    }

    private void registerPurchase(Customer customer, String amount) {
        PurchaseForm form = new PurchaseForm();
        form.setCustomerId(customer.getId());
        form.setAmount(new BigDecimal(amount));
        purchaseService.register(form);
    }

    private CustomerForm customerForm(String name, String email) {
        CustomerForm form = new CustomerForm();
        form.setName(name);
        form.setEmail(email);
        return form;
    }
}
