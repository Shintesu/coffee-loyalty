package com.example.coffeeloyalty.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.coffeeloyalty.entity.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findAllByOrderByCreatedAtDesc();

    List<Purchase> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
