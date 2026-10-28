package com.example.coffeeloyalty.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.coffeeloyalty.dto.RedemptionForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Redemption;
import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.exception.CustomerNotFoundException;
import com.example.coffeeloyalty.exception.InactiveRewardException;
import com.example.coffeeloyalty.exception.InsufficientPointsException;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.RedemptionRepository;

@Service
public class RedemptionService {

    private static final Logger log = LoggerFactory.getLogger(RedemptionService.class);

    private final RedemptionRepository redemptionRepository;
    private final CustomerRepository customerRepository;
    private final RewardService rewardService;
    private final Clock clock;

    public RedemptionService(RedemptionRepository redemptionRepository, CustomerRepository customerRepository,
            RewardService rewardService, Clock clock) {
        this.redemptionRepository = redemptionRepository;
        this.customerRepository = customerRepository;
        this.rewardService = rewardService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Redemption> findAll() {
        return redemptionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Redemption> findByCustomerId(Long customerId) {
        return redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public Redemption redeem(RedemptionForm form) {
        Customer customer = customerRepository.findByIdForUpdate(form.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(form.getCustomerId()));
        Reward reward = rewardService.findById(form.getRewardId());

        if (!reward.isActive()) {
            throw new InactiveRewardException(reward.getName());
        }
        if (customer.getPoints() < reward.getRequiredPoints()) {
            throw new InsufficientPointsException(customer.getName(), customer.getPoints(),
                    reward.getName(), reward.getRequiredPoints());
        }

        customer.setPoints(customer.getPoints() - reward.getRequiredPoints());
        Redemption redemption = new Redemption(customer, reward, reward.getRequiredPoints(), LocalDateTime.now(clock));

        redemptionRepository.save(redemption);
        customerRepository.save(customer);

        log.debug("redeemed reward {} for customer {} spending {} points",
                reward.getId(), customer.getId(), redemption.getPointsSpent());
        return redemption;
    }
}
