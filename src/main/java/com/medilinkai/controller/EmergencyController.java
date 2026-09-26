package com.medilinkai.controller;

import com.medilinkai.model.Medicine;
import com.medilinkai.model.Stock;
import com.medilinkai.service.GeoService;
import com.medilinkai.service.MedicineService;
import com.medilinkai.service.StockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature 5: Emergency Mode.
 * Patient types a medicine + their area. We geocode the area (free Nominatim
 * API), find pharmacies that actually have the medicine in stock, sort them
 * by distance, and show the nearest one on an OpenStreetMap embed.
 */
@Controller
public class EmergencyController {

    private final MedicineService medicineService;
    private final StockService stockService;
    private final GeoService geoService;

    public EmergencyController(MedicineService medicineService, StockService stockService,
                               GeoService geoService) {
        this.medicineService = medicineService;
        this.stockService = stockService;
        this.geoService = geoService;
    }

    @GetMapping("/emergency")
    public String form(Model model) {
        model.addAttribute("medicines", medicineService.findAll());
        return "emergency-form";
    }

    @PostMapping("/emergency")
    public String find(@RequestParam Long medicineId,
                       @RequestParam(defaultValue = "Dhaka") String area,
                       Model model) {
        Medicine medicine = medicineService.findById(medicineId);
        if (medicine == null) {
            return "redirect:/emergency";
        }

        // 1) where is the patient?
        double[] here = geoService.geocode(area);

        // 2) which pharmacies have it in stock, and how far are they?
        List<Stock> inStock = stockService.inStockForMedicine(medicineId);
        Map<Long, Double> distances = new HashMap<>();
        for (Stock s : inStock) {
            double km = geoService.distanceKm(here[0], here[1],
                    s.getPharmacy().getLat(), s.getPharmacy().getLng());
            distances.put(s.getId(), Math.round(km * 10.0) / 10.0);
        }
        inStock.sort(Comparator.comparingDouble(s -> distances.get(s.getId())));

        model.addAttribute("medicine", medicine);
        model.addAttribute("area", area);
        model.addAttribute("stocks", inStock);
        model.addAttribute("distances", distances);
        model.addAttribute("medicines", medicineService.findAll());

        // 3) OpenStreetMap embed centred on the nearest pharmacy (no JS needed)
        if (!inStock.isEmpty()) {
            var nearest = inStock.get(0).getPharmacy();
            double d = 0.02; // map window size
            String mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox="
                    + (nearest.getLng() - d) + "%2C" + (nearest.getLat() - d) + "%2C"
                    + (nearest.getLng() + d) + "%2C" + (nearest.getLat() + d)
                    + "&layer=mapnik&marker=" + nearest.getLat() + "%2C" + nearest.getLng();
            model.addAttribute("mapUrl", mapUrl);
            model.addAttribute("nearest", nearest);
            // share link for emergency contacts
            model.addAttribute("shareUrl", "https://www.openstreetmap.org/?mlat="
                    + nearest.getLat() + "&mlon=" + nearest.getLng() + "#map=16/"
                    + nearest.getLat() + "/" + nearest.getLng());
        }
        return "emergency-result";
    }
}
