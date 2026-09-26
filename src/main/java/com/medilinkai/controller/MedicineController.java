package com.medilinkai.controller;

import com.medilinkai.model.Medicine;
import com.medilinkai.service.MedicineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feature 2: search a brand, see its generic, and find cheaper alternatives.
 */
@Controller
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping("/medicines")
    public String search(@RequestParam(required = false) String q, Model model) {
        List<Medicine> results = medicineService.searchByBrand(q);
        model.addAttribute("q", q);
        model.addAttribute("results", results);
        return "medicine-search";
    }

    @GetMapping("/medicines/{id}/alternatives")
    public String alternatives(@PathVariable Long id, Model model) {
        Medicine medicine = medicineService.findById(id);
        if (medicine == null) {
            return "redirect:/medicines";
        }
        List<Medicine> alternatives = medicineService.findAlternatives(medicine);
        model.addAttribute("medicine", medicine);
        model.addAttribute("alternatives", alternatives);
        return "medicine-alternatives";
    }
}
