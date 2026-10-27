package com.example.coffeeloyalty.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.service.CustomerService;
import com.example.coffeeloyalty.service.PurchaseService;

@Controller
@RequestMapping("/points")
public class PointsController {

    private final CustomerService customerService;
    private final PurchaseService purchaseService;

    public PointsController(CustomerService customerService, PurchaseService purchaseService) {
        this.customerService = customerService;
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("customers", customerService.findAll());
        return "points/list";
    }

    @GetMapping("/{customerId}")
    public String detail(@PathVariable Long customerId, Model model) {
        Customer customer = customerService.findById(customerId);
        model.addAttribute("customer", customer);
        model.addAttribute("purchases", purchaseService.findByCustomerId(customerId));
        return "points/detail";
    }
}
