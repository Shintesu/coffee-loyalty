package com.example.coffeeloyalty.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.repository.RewardRepository;

@Component
@Profile("!test")
@Order(1)
public class RewardSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RewardSeeder.class);

    private final RewardRepository rewardRepository;

    public RewardSeeder(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @Override
    public void run(String... args) {
        if (rewardRepository.count() == 0) {
            List<Reward> rewards = List.of(
                    new Reward("Café americano", "Un café americano de la casa.", 10, true),
                    new Reward("Cappuccino", "Un cappuccino de la casa.", 20, true),
                    new Reward("Postre del día", "Una porción del postre del día.", 30, true),
                    new Reward("Combo de café", "Un café y un postre a elección.", 50, true));
            rewardRepository.saveAll(rewards);
            log.info("seeded {} rewards", rewards.size());
        }
    }
}
