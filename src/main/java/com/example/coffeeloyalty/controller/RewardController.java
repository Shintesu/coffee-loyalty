package com.example.coffeeloyalty.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.coffeeloyalty.service.RewardService;

@Controller
@RequestMapping("/rewards")
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rewards", rewardService.findActive());
        return "rewards/list";
    }
}
