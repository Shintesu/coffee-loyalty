package com.example.coffeeloyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.coffeeloyalty.dto.PurchaseForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Purchase;
import com.example.coffeeloyalty.exception.CustomerNotFoundException;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.PurchaseRepository;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-26T15:30:00Z"), ZoneOffset.UTC);

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private CustomerRepository customerRepository;

    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(purchaseRepository, customerRepository, FIXED_CLOCK);
    }

    @ParameterizedTest
    @CsvSource({
            "0.01, 0",
            "0.49, 0",
            "0.50, 1",
            "1.00, 1",
            "9.49, 9",
            "9.99, 10",
            "10.00, 10",
            "25.99, 26",
            "1000.00, 1000"
    })
    void roundsPurchaseAmountToTheNearestDollar(String amount, int expectedPoints) {
        Customer customer = customer(1L, 0);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Purchase purchase = purchaseService.register(form(1L, amount));

        assertThat(purchase.getPointsEarned()).isEqualTo(expectedPoints);
        assertThat(customer.getPoints()).isEqualTo(expectedPoints);
    }

    @Test
    void registerAddsPointsToBalanceAndSavesPurchaseWithClockTimestamp() {
        Customer customer = customer(1L, 5);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Purchase purchase = purchaseService.register(form(1L, "9.99"));

        assertThat(purchase.getCustomer()).isSameAs(customer);
        assertThat(purchase.getAmount()).isEqualByComparingTo("9.99");
        assertThat(purchase.getPointsEarned()).isEqualTo(10);
        assertThat(purchase.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 26, 15, 30));
        assertThat(customer.getPoints()).isEqualTo(15);
        verify(purchaseRepository).save(purchase);
        verify(customerRepository).save(customer);
    }

    @Test
    void registerUsesFindByIdForUpdateInsteadOfFindById() {
        Customer customer = customer(1L, 0);
        when(customerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(customer));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        purchaseService.register(form(1L, "1.00"));

        verify(customerRepository).findByIdForUpdate(1L);
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void throwsWhenCustomerDoesNotExistAndSavesNothing() {
        when(customerRepository.findByIdForUpdate(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.register(form(7L, "9.99")))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("No existe el cliente con id 7.");

        verify(purchaseRepository, never()).save(any(Purchase.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    private PurchaseForm form(Long customerId, String amount) {
        PurchaseForm form = new PurchaseForm();
        form.setCustomerId(customerId);
        form.setAmount(new BigDecimal(amount));
        return form;
    }

    private Customer customer(Long id, int points) {
        Customer customer = new Customer("Ana Torres", "ana@example.com", LocalDate.of(2026, 1, 10), points);
        customer.setId(id);
        return customer;
    }
}
