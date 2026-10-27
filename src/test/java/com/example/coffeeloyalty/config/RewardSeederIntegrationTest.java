package com.example.coffeeloyalty.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.repository.RewardRepository;

@SpringBootTest
@ActiveProfiles("test")
class RewardSeederIntegrationTest {

    private final RewardRepository rewardRepository;

    @Autowired
    RewardSeederIntegrationTest(RewardRepository rewardRepository) {
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
    void seedsFourRewardsOnlyOnce() {
        RewardSeeder rewardSeeder = new RewardSeeder(rewardRepository);

        rewardSeeder.run();
        rewardSeeder.run();

        List<Reward> rewards = rewardRepository.findAll();
        assertThat(rewards).hasSize(4);
        assertThat(rewards).extracting(Reward::getRequiredPoints).containsExactlyInAnyOrder(10, 20, 30, 50);
        assertThat(rewards).allMatch(Reward::isActive);
    }
}
