package com.example.coffeeloyalty.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.coffeeloyalty.entity.Reward;

@SpringBootTest
@ActiveProfiles("test")
class RewardRepositoryIntegrationTest {

    private final RewardRepository rewardRepository;

    @Autowired
    RewardRepositoryIntegrationTest(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @BeforeEach
    void cleanBefore() {
        rewardRepository.deleteAll();
    }

    @AfterEach
    void cleanAfter() {
        rewardRepository.deleteAll();
    }

    @Test
    void returnsOnlyActiveRewards() {
        rewardRepository.saveAll(List.of(
                new Reward("Active reward", "Available to redeem.", 10, true),
                new Reward("Inactive reward", "Not available to redeem.", 20, false)));

        List<Reward> rewards = rewardRepository.findByActiveTrueOrderByRequiredPointsAsc();

        assertThat(rewards).extracting(Reward::getName).containsExactly("Active reward");
    }

    @Test
    void ordersActiveRewardsByRequiredPointsAscending() {
        rewardRepository.saveAll(List.of(
                new Reward("Fifty points", "Costs fifty points.", 50, true),
                new Reward("Ten points", "Costs ten points.", 10, true),
                new Reward("Thirty points", "Costs thirty points.", 30, true)));

        List<Reward> rewards = rewardRepository.findByActiveTrueOrderByRequiredPointsAsc();

        assertThat(rewards).extracting(Reward::getRequiredPoints).containsExactly(10, 30, 50);
    }
}
