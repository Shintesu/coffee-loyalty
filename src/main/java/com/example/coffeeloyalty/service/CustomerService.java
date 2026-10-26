package com.example.coffeeloyalty.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.coffeeloyalty.dto.CustomerForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.exception.CustomerNotFoundException;
import com.example.coffeeloyalty.exception.DuplicateEmailException;
import com.example.coffeeloyalty.repository.CustomerRepository;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final Clock clock;

    public CustomerService(CustomerRepository customerRepository, Clock clock) {
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional
    public Customer create(CustomerForm form) {
        String email = normalizeEmail(form.getEmail());
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        Customer customer = new Customer(form.getName().trim(), email, LocalDate.now(clock), 0);
        try {
            customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException(email, ex);
        }
        log.debug("created customer {}", customer.getId());
        return customer;
    }

    @Transactional
    public Customer update(Long id, CustomerForm form) {
        Customer customer = findById(id);
        String email = normalizeEmail(form.getEmail());
        if (customerRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }

        customer.setName(form.getName().trim());
        customer.setEmail(email);
        try {
            customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException(email, ex);
        }
        log.debug("updated customer {}", customer.getId());
        return customer;
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findById(id);
        customerRepository.delete(customer);
        log.debug("deleted customer {}", id);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
