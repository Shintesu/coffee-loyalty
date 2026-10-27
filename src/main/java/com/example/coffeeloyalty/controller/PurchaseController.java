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

import com.example.coffeeloyalty.dto.PurchaseForm;
import com.example.coffeeloyalty.entity.Purchase;
import com.example.coffeeloyalty.service.CustomerService;
import com.example.coffeeloyalty.service.PurchaseService;

@Controller
@RequestMapping("/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final CustomerService customerService;

    public PurchaseController(PurchaseService purchaseService, CustomerService customerService) {
        this.purchaseService = purchaseService;
        this.customerService = customerService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("purchases", purchaseService.findAll());
        return "purchases/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(name = "customerId", required = false) Long customerId, Model model) {
        PurchaseForm form = new PurchaseForm();
        form.setCustomerId(customerId);
        model.addAttribute("purchaseForm", form);
        model.addAttribute("customers", customerService.findAll());
        return "purchases/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("purchaseForm") PurchaseForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customers", customerService.findAll());
            return "purchases/form";
        }

        Purchase purchase = purchaseService.register(form);
        redirectAttributes.addFlashAttribute("message",
                "Compra registrada correctamente. Puntos obtenidos: "
                        + purchase.getPointsEarned() + ". Saldo actual: "
                        + purchase.getCustomer().getPoints() + ".");
        return "redirect:/points/" + purchase.getCustomer().getId();
    }
}
