package com.example.coffeeloyalty.service;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.coffeeloyalty.dto.PurchaseForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Purchase;
import com.example.coffeeloyalty.exception.CustomerNotFoundException;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.PurchaseRepository;

@Service
public class PurchaseService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseService.class);

    private final PurchaseRepository purchaseRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    public PurchaseService(PurchaseRepository purchaseRepository, CustomerRepository customerRepository, Clock clock) {
        this.purchaseRepository = purchaseRepository;
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Purchase> findAll() {
        return purchaseRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Purchase> findByCustomerId(Long customerId) {
        return purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public Purchase register(PurchaseForm form) {
        // Lock the customer row so concurrent movements read and update the same balance (SPECS §15).
        Customer customer = customerRepository.findByIdForUpdate(form.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(form.getCustomerId()));

        int earnedPoints = form.getAmount().setScale(0, RoundingMode.HALF_UP).intValueExact();
        Purchase purchase = new Purchase(customer, form.getAmount(), earnedPoints, LocalDateTime.now(clock));

        customer.setPoints(customer.getPoints() + earnedPoints);
        purchaseRepository.save(purchase);
        customerRepository.save(customer);

        log.debug("registered purchase {} for customer {} with {} points",
                purchase.getId(), customer.getId(), earnedPoints);
        return purchase;
    }
}
