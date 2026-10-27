package com.example.coffeeloyalty.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.coffeeloyalty.entity.Reward;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findByActiveTrueOrderByRequiredPointsAsc();
}
