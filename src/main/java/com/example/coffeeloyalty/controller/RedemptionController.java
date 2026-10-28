package com.example.coffeeloyalty.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.coffeeloyalty.dto.RedemptionForm;
import com.example.coffeeloyalty.entity.Redemption;
import com.example.coffeeloyalty.exception.InactiveRewardException;
import com.example.coffeeloyalty.exception.InsufficientPointsException;
import com.example.coffeeloyalty.service.CustomerService;
import com.example.coffeeloyalty.service.RedemptionService;
import com.example.coffeeloyalty.service.RewardService;

@Controller
@RequestMapping("/redemptions")
public class RedemptionController {

    private final RedemptionService redemptionService;
    private final CustomerService customerService;
    private final RewardService rewardService;

    public RedemptionController(RedemptionService redemptionService, CustomerService customerService,
            RewardService rewardService) {
        this.redemptionService = redemptionService;
        this.customerService = customerService;
        this.rewardService = rewardService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("redemptions", redemptionService.findAll());
        return "redemptions/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(name = "rewardId", required = false) Long rewardId,
            @RequestParam(name = "customerId", required = false) Long customerId, Model model) {
        RedemptionForm form = new RedemptionForm();
        form.setRewardId(rewardId);
        form.setCustomerId(customerId);
        model.addAttribute("redemptionForm", form);
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("rewards", rewardService.findActive());
        return "redemptions/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("redemptionForm") RedemptionForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateForm(model);
            return "redemptions/form";
        }

        Redemption redemption;
        try {
            redemption = redemptionService.redeem(form);
        } catch (InactiveRewardException | InsufficientPointsException ex) {
            bindingResult.reject("redemption", ex.getMessage());
            populateForm(model);
            return "redemptions/form";
        }

        redirectAttributes.addFlashAttribute("message",
                "Canje registrado correctamente. Recompensa: " + redemption.getReward().getName()
                        + ". Puntos gastados: " + redemption.getPointsSpent()
                        + ". Saldo actual: " + redemption.getCustomer().getPoints() + ".");
        return "redirect:/points/" + redemption.getCustomer().getId();
    }

    private void populateForm(Model model) {
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("rewards", rewardService.findActive());
    }
}
