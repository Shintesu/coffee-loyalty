package com.example.coffeeloyalty.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.coffeeloyalty.exception.CustomerNotFoundException;
import com.example.coffeeloyalty.exception.RewardNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleCustomerNotFound(CustomerNotFoundException ex, Model model) {
        model.addAttribute("businessMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(RewardNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleRewardNotFound(RewardNotFoundException ex, Model model) {
        model.addAttribute("businessMessage", ex.getMessage());
        return "error";
    }
}
