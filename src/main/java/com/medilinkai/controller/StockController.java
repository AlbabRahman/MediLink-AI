package com.medilinkai.controller;

import com.medilinkai.model.Medicine;
import com.medilinkai.model.Pharmacy;
import com.medilinkai.model.Stock;
import com.medilinkai.model.User;
import com.medilinkai.repository.PharmacyRepository;
import com.medilinkai.service.MedicineService;
import com.medilinkai.service.StockService;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feature 3: pharmacist updates stock (plain form posts), patients watch
 * the live page which receives WebSocket pushes.
 */
@Controller
public class StockController {

    private final StockService stockService;
    private final PharmacyRepository pharmacyRepo;
    private final MedicineService medicineService;
    private final UserService userService;

    public StockController(StockService stockService, PharmacyRepository pharmacyRepo,
                           MedicineService medicineService, UserService userService) {
        this.stockService = stockService;
        this.pharmacyRepo = pharmacyRepo;
        this.medicineService = medicineService;
        this.userService = userService;
    }

    /** Pharmacist's stock management page. */
    @GetMapping("/pharmacy/stock")
    public String myStock(HttpSession session, Model model) {
        User user = currentUser(session);
        List<Pharmacy> owned = pharmacyRepo.findByOwnerId(user.getId());
        if (owned.isEmpty()) {
            model.addAttribute("noPharmacy", true);
            return "pharmacy-stock";
        }
        Pharmacy pharmacy = owned.get(0);
        List<Stock> stockList = stockService.stockOfPharmacy(pharmacy.getId());
        model.addAttribute("user", user);
        model.addAttribute("pharmacy", pharmacy);
        model.addAttribute("stockList", stockList);
        return "pharmacy-stock";
    }

    /** Pharmacist saves a new quantity -> service saves + pushes live update. */
    @PostMapping("/pharmacy/stock/{stockId}")
    public String updateStock(@PathVariable Long stockId, @RequestParam int quantity) {
        stockService.updateQuantity(stockId, Math.max(0, quantity));
        return "redirect:/pharmacy/stock";
    }

    /**
     * Patient's live stock page: search a medicine, see which pharmacies
     * have it, and watch quantities change in real time at the top.
     */
    @GetMapping("/stock/live")
    public String liveStock(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q);
        if (q != null && !q.isBlank()) {
            List<Medicine> matches = medicineService.searchByBrand(q);
            model.addAttribute("matches", matches);
            if (!matches.isEmpty()) {
                model.addAttribute("stocks", stockService.inStockForMedicine(matches.get(0).getId()));
                model.addAttribute("selected", matches.get(0));
            }
        }
        return "stock-live";
    }

    private User currentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return userService.findById(userId).orElseThrow();
    }
}
