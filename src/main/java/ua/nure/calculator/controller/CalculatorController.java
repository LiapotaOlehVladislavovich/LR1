package ua.nure.calculator.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ua.nure.calculator.service.CalculatorService;

@RestController
@RequestMapping("/api")
public class CalculatorController {

    @Autowired
    private CalculatorService calculatorService;

    @PostMapping("/calculate")
    public String calculate(@RequestBody String expression) {
        return calculatorService.calculate(expression);
    }
}
