package com.medilinkai.controller;

import com.medilinkai.service.BatchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feature 6: fake medicine detection.
 * Patient types (or scans into the box) the QR/barcode printed on the pack.
 */
@Controller
public class VerifyController {

    private final BatchService batchService;

    public VerifyController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping("/verify")
    public String form() {
        return "verify-form";
    }

    @PostMapping("/verify")
    public String check(@RequestParam String code, Model model) {
        BatchService.CheckResult result = batchService.check(code);
        model.addAttribute("code", code);
        model.addAttribute("result", result);
        return "verify-result";
    }
}
