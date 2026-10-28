package com.example.coffeeloyalty.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.exception.RewardNotFoundException;
import com.example.coffeeloyalty.repository.RewardRepository;

@Service
public class RewardService {

    private final RewardRepository rewardRepository;

    public RewardService(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @Transactional(readOnly = true)
    public List<Reward> findActive() {
        return rewardRepository.findByActiveTrueOrderByRequiredPointsAsc();
    }

    @Transactional(readOnly = true)
    public Reward findById(Long id) {
        return rewardRepository.findById(id)
                .orElseThrow(() -> new RewardNotFoundException(id));
    }
}
