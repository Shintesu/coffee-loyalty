package com.example.coffeeloyalty.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.coffeeloyalty.entity.Redemption;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    List<Redemption> findAllByOrderByCreatedAtDesc();

    List<Redemption> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
