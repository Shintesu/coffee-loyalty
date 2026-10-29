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
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.coffeeloyalty.dto.CustomerForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.exception.CustomerNotFoundException;
import com.example.coffeeloyalty.exception.DuplicateEmailException;
import com.example.coffeeloyalty.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-26T15:30:00Z"), ZoneOffset.UTC);

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, FIXED_CLOCK);
    }

    @Test
    void createsCustomerWithJoinDateFromClockAndZeroPoints() {
        CustomerForm form = form("Ana Torres", "  Ana@Example.COM  ");
        when(customerRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = customerService.create(form);

        assertThat(customer.getName()).isEqualTo("Ana Torres");
        assertThat(customer.getEmail()).isEqualTo("ana@example.com");
        assertThat(customer.getJoinDate()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(customer.getPoints()).isZero();
        verify(customerRepository).saveAndFlush(any(Customer.class));
    }

    @Test
    void trimsNameWhenCreatingCustomer() {
        CustomerForm form = form("  Ana Torres  ", "ana@example.com");
        when(customerRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = customerService.create(form);

        assertThat(customer.getName()).isEqualTo("Ana Torres");
    }

    @Test
    void rejectsDuplicateEmailRegardlessOfCase() {
        CustomerForm form = form("Ana Torres", "ANA@EXAMPLE.COM");
        when(customerRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(form))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Ya existe un cliente con el email ana@example.com.");

        verify(customerRepository, never()).saveAndFlush(any(Customer.class));
    }

    @Test
    void updatesCustomerExcludingItsOwnIdFromEmailCheck() {
        Customer customer = customer(1L, "Ana Torres", "ana@example.com", 25, LocalDate.of(2026, 1, 10));
        CustomerForm form = form("Ana Torres Gómez", "ANA@EXAMPLE.COM");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndIdNot("ana@example.com", 1L)).thenReturn(false);
        when(customerRepository.saveAndFlush(customer)).thenReturn(customer);

        Customer updated = customerService.update(1L, form);

        assertThat(updated.getName()).isEqualTo("Ana Torres Gómez");
        assertThat(updated.getEmail()).isEqualTo("ana@example.com");
        assertThat(updated.getPoints()).isEqualTo(25);
        assertThat(updated.getJoinDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        verify(customerRepository).existsByEmailAndIdNot("ana@example.com", 1L);
    }

    @Test
    void trimsNameWhenUpdatingCustomer() {
        Customer customer = customer(1L, "Ana Torres", "ana@example.com", 0, LocalDate.of(2026, 1, 10));
        CustomerForm form = form("  Ana Torres Gómez  ", "ana@example.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndIdNot("ana@example.com", 1L)).thenReturn(false);
        when(customerRepository.saveAndFlush(customer)).thenReturn(customer);

        Customer updated = customerService.update(1L, form);

        assertThat(updated.getName()).isEqualTo("Ana Torres Gómez");
    }

    @Test
    void rejectsEditWhenEmailBelongsToAnotherCustomer() {
        Customer customer = customer(1L, "Ana Torres", "ana@example.com", 0, LocalDate.of(2026, 1, 10));
        CustomerForm form = form("Ana Torres", "luis@example.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndIdNot("luis@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(1L, form))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Ya existe un cliente con el email luis@example.com.");

        assertThat(customer.getEmail()).isEqualTo("ana@example.com");
        verify(customerRepository, never()).saveAndFlush(any(Customer.class));
    }

    @Test
    void throwsWhenCustomerDoesNotExist() {
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(7L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("No existe el cliente con id 7.");
    }

    @Test
    void translatesDuplicateEmailConstraintViolation() {
        CustomerForm form = form("Ana Torres", "ana@example.com");
        DataIntegrityViolationException constraintViolation = new DataIntegrityViolationException("duplicate email");
        when(customerRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class))).thenThrow(constraintViolation);

        assertThatThrownBy(() -> customerService.create(form))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Ya existe un cliente con el email ana@example.com.")
                .hasCause(constraintViolation);
    }

    @Test
    void deletesExistingCustomer() {
        Customer customer = customer(1L, "Ana Torres", "ana@example.com", 0, LocalDate.of(2026, 1, 10));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.delete(1L);

        verify(customerRepository).delete(customer);
    }

    @Test
    void throwsWhenDeletingCustomerThatDoesNotExist() {
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(7L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("No existe el cliente con id 7.");

        verify(customerRepository, never()).delete(any(Customer.class));
    }

    @Test
    void translatesDuplicateEmailConstraintViolationWhenUpdatingCustomer() {
        Customer customer = customer(1L, "Ana Torres", "ana@example.com", 0, LocalDate.of(2026, 1, 10));
        CustomerForm form = form("Ana Torres", "luis@example.com");
        DataIntegrityViolationException constraintViolation = new DataIntegrityViolationException("duplicate email");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndIdNot("luis@example.com", 1L)).thenReturn(false);
        when(customerRepository.saveAndFlush(customer)).thenThrow(constraintViolation);

        assertThatThrownBy(() -> customerService.update(1L, form))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("Ya existe un cliente con el email luis@example.com.")
                .hasCause(constraintViolation);
    }

    @Test
    void throwsWhenUpdatingCustomerThatDoesNotExist() {
        CustomerForm form = form("Ana Torres", "ana@example.com");
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(7L, form))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("No existe el cliente con id 7.");

        verify(customerRepository, never()).existsByEmailAndIdNot(any(), any());
        verify(customerRepository, never()).saveAndFlush(any(Customer.class));
    }

    private CustomerForm form(String name, String email) {
        CustomerForm form = new CustomerForm();
        form.setName(name);
        form.setEmail(email);
        return form;
    }

    private Customer customer(Long id, String name, String email, int points, LocalDate joinDate) {
        Customer customer = new Customer(name, email, joinDate, points);
        customer.setId(id);
        return customer;
    }
}
