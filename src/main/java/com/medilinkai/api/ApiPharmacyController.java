package com.medilinkai.api;

import com.medilinkai.model.*;
import com.medilinkai.repository.PharmacyRepository;
import com.medilinkai.repository.StockRepository;
import com.medilinkai.service.GeoService;
import com.medilinkai.service.StockService;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/** Pharmacy stock + emergency finder endpoints for the portal. */
@RestController
public class ApiPharmacyController {

    private final StockRepository stockRepo;
    private final PharmacyRepository pharmacyRepo;
    private final StockService stockService;
    private final GeoService geoService;
    private final UserService userService;
    private final ApiEventBus eventBus;

    public ApiPharmacyController(StockRepository stockRepo, PharmacyRepository pharmacyRepo,
                                 StockService stockService, GeoService geoService,
                                 UserService userService, ApiEventBus eventBus) {
        this.stockRepo = stockRepo;
        this.pharmacyRepo = pharmacyRepo;
        this.stockService = stockService;
        this.geoService = geoService;
        this.userService = userService;
        this.eventBus = eventBus;
    }

    @GetMapping("/api/pharmacies/stocks")
    public Map<String, Object> stocks() {
        List<Stock> all = stockRepo.findAll();
        List<Map<String, Object>> rows = all.stream().map(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", String.valueOf(s.getId()));
            row.put("medicineBrandName", s.getMedicine().getBrandName() + " " + s.getMedicine().getStrength());
            row.put("genericName", s.getMedicine().getGeneric() == null ? "" : s.getMedicine().getGeneric().getName());
            row.put("pharmacyName", s.getPharmacy().getName());
            row.put("quantity", s.getQuantity());
            row.put("unitPrice", s.getMedicine().getPriceBdt());
            return row;
        }).collect(Collectors.toList());
        return Map.of("stocks", rows);
    }

    public record StockUpdateRequest(String stockId, Integer quantity) {
    }

    @PostMapping("/api/pharmacies/stock/update")
    public Map<String, Object> updateStock(@RequestBody StockUpdateRequest req, HttpSession session) {
        User me = ApiSession.current(userService, session);
        if (me == null) {
            return Map.of("status", "ERROR", "message", "Not signed in.");
        }
        try {
            Stock saved = stockService.updateQuantity(Long.valueOf(req.stockId()),
                    Math.max(0, req.quantity() == null ? 0 : req.quantity()));
            eventBus.publish("STOCK_UPDATE: " + saved.getMedicine().getBrandName() + " "
                    + saved.getMedicine().getStrength() + " at " + saved.getPharmacy().getName()
                    + " now " + saved.getQuantity() + " units");
            return Map.of("status", "SUCCESS");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "Stock item not found.");
        }
    }

    @GetMapping("/api/pharmacies/emergency")
    public Map<String, Object> emergency(@RequestParam(defaultValue = "23.7465") double lat,
                                         @RequestParam(defaultValue = "90.3760") double lng) {
        record Row(Pharmacy p, double km) {
        }
        List<Row> rows = pharmacyRepo.findAll().stream()
                .map(p -> new Row(p, geoService.distanceKm(lat, lng, p.getLat(), p.getLng())))
                .sorted(Comparator.comparingDouble(Row::km))
                .toList();

        List<Map<String, Object>> out = rows.stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", r.p().getName());
            row.put("address", r.p().getAddress());
            row.put("area", r.p().getArea());
            row.put("distanceKm", Math.round(r.km() * 10.0) / 10.0);
            row.put("is24Hours", r.p().isOpen24h());
            row.put("phone", r.p().getPhone() == null || r.p().getPhone().isBlank()
                    ? "+8801711001122" : r.p().getPhone());
            row.put("lat", r.p().getLat());
            row.put("lng", r.p().getLng());
            return row;
        }).collect(Collectors.toList());
        return Map.of("emergencyPharmacies", out);
    }
}
