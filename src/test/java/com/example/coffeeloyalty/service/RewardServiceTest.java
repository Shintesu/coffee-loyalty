package com.example.coffeeloyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.exception.RewardNotFoundException;
import com.example.coffeeloyalty.repository.RewardRepository;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardRepository rewardRepository;

    private RewardService rewardService;

    @BeforeEach
    void setUp() {
        rewardService = new RewardService(rewardRepository);
    }

    @Test
    void returnsRewardWhenItExists() {
        Reward reward = new Reward("Postre del día", "Una porción del postre del día.", 30, true);
        when(rewardRepository.findById(7L)).thenReturn(Optional.of(reward));

        assertThat(rewardService.findById(7L)).isSameAs(reward);
    }

    @Test
    void throwsWhenRewardDoesNotExist() {
        when(rewardRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.findById(7L))
                .isInstanceOf(RewardNotFoundException.class)
                .hasMessage("No existe la recompensa con id 7.");
    }
}
