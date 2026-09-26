package com.medilinkai.controller;

import com.medilinkai.repository.MedicineBatchRepository;
import com.medilinkai.repository.PharmacyRepository;
import com.medilinkai.service.MedicineService;
import com.medilinkai.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Simple admin dashboard: overview of users, medicines, pharmacies, batches.
 */
@Controller
public class AdminController {

    private final UserService userService;
    private final MedicineService medicineService;
    private final PharmacyRepository pharmacyRepo;
    private final MedicineBatchRepository batchRepo;

    public AdminController(UserService userService, MedicineService medicineService,
                           PharmacyRepository pharmacyRepo, MedicineBatchRepository batchRepo) {
        this.userService = userService;
        this.medicineService = medicineService;
        this.pharmacyRepo = pharmacyRepo;
        this.batchRepo = batchRepo;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("medicines", medicineService.findAll());
        model.addAttribute("pharmacies", pharmacyRepo.findAll());
        model.addAttribute("batches", batchRepo.findAll());
        return "admin";
    }
}
