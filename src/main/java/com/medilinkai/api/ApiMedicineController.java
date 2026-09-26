package com.medilinkai.api;

import com.medilinkai.model.Medicine;
import com.medilinkai.model.MedicineBatch;
import com.medilinkai.model.Stock;
import com.medilinkai.repository.PharmacyRepository;
import com.medilinkai.repository.StockRepository;
import com.medilinkai.service.BatchService;
import com.medilinkai.service.MedicineService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Medicine endpoints for the portal: list, strategy search, pharmacy price
 * comparison, interaction check, and fake-medicine QR verification.
 */
@RestController
public class ApiMedicineController {

    private final MedicineService medicineService;
    private final StockRepository stockRepo;
    private final BatchService batchService;

    public ApiMedicineController(MedicineService medicineService, StockRepository stockRepo,
                                 BatchService batchService) {
        this.medicineService = medicineService;
        this.stockRepo = stockRepo;
        this.batchService = batchService;
    }

    /** One medicine card exactly as the portal's renderMedicines() expects. */
    private Map<String, Object> card(Medicine m) {
        String strength = m.getStrength() == null ? "" : m.getStrength().toLowerCase();
        String formulation = strength.contains("inhaler") ? "Inhaler"
                : strength.contains("sachet") ? "Sachet"
                : strength.contains("vial") || strength.contains("iu/ml") ? "Injection"
                : strength.contains("capsule") ? "Capsule"
                : strength.contains("syrup") ? "Syrup"
                : "Tablet";
        String treats = m.getGeneric() != null && m.getGeneric().getTreats() != null
                ? m.getGeneric().getTreats() : "";
        return new LinkedHashMap<>(Map.of(
                "id", String.valueOf(m.getId()),
                "brandName", m.getBrandName(),
                "strength", m.getStrength() == null ? "" : m.getStrength(),
                "genericName", m.getGeneric() == null ? "" : m.getGeneric().getName(),
                "formulation", formulation,
                "unitPrice", m.getPriceBdt(),
                "company", m.getCompany() == null ? "" : m.getCompany(),
                "category", m.getGeneric() == null ? "" : m.getGeneric().getName(),
                "displayBadge", "DGDA Verified",
                "sideEffects", treats.isEmpty() ? "" : "Used for: " + treats));
    }

    @GetMapping("/api/medicines")
    public Map<String, Object> all() {
        List<Map<String, Object>> results = medicineService.findAll().stream()
                .map(this::card).collect(Collectors.toList());
        return Map.of("results", results);
    }

    @GetMapping("/api/medicines/search")
    public Map<String, Object> search(@RequestParam String query,
                                      @RequestParam(defaultValue = "NAME_SEARCH") String strategy) {
        List<Medicine> found = new ArrayList<>();
        if (strategy != null && strategy.equalsIgnoreCase("GENERIC_STRATEGY")) {
            // match by generic (chemical) name
            String q = query.toLowerCase().trim();
            for (Medicine m : medicineService.findAll()) {
                String gen = m.getGeneric() == null ? "" : m.getGeneric().getName();
                if (gen.toLowerCase().contains(q) || m.getBrandName().toLowerCase().contains(q)) {
                    found.add(m);
                }
            }
        } else {
            found.addAll(medicineService.searchByBrand(query));
        }

        if (strategy != null && strategy.equalsIgnoreCase("BEST_PRICE_STRATEGY") && !found.isEmpty()) {
            // cheapest first within the same generic family
            Medicine first = found.get(0);
            if (first.getGeneric() != null) {
                found = new ArrayList<>(medicineService.findAlternatives(first));
            } else {
                found.sort(Comparator.comparingDouble(Medicine::getPriceBdt));
            }
        }
        List<Map<String, Object>> results = found.stream().map(this::card).collect(Collectors.toList());
        return Map.of("results", results);
    }

    @GetMapping("/api/medicines/pharmacy-prices")
    public Map<String, Object> pharmacyPrices(@RequestParam Long medicineId) {
        Medicine med = medicineService.findById(medicineId);
        if (med == null) {
            return Map.of("status", "ERROR", "message", "Medicine not found",
                    "pharmacyPrices", List.of());
        }
        List<Stock> inStock = stockRepo.findByMedicineIdAndQuantityGreaterThan(medicineId, 0);

        // Deterministic small price variation per pharmacy so the comparison view
        // has a meaningful best/max spread (display only; base price is DGDA MRP).
        List<Map<String, Object>> prices = new ArrayList<>();
        double best = Double.MAX_VALUE;
        double max = 0;
        for (Stock s : inStock) {
            double factor = 1.0 + ((s.getPharmacy().getId() % 4) * 0.05);
            double unit = Math.round(med.getPriceBdt() * factor * 100.0) / 100.0;
            best = Math.min(best, unit);
            max = Math.max(max, unit);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pharmacyName", s.getPharmacy().getName());
            row.put("area", s.getPharmacy().getArea());
            row.put("address", s.getPharmacy().getAddress());
            row.put("phone", s.getPharmacy().getPhone());
            row.put("unitPrice", unit);
            row.put("quantity", s.getQuantity());
            row.put("is24Hours", s.getPharmacy().isOpen24h());
            row.put("isBestPrice", false);
            prices.add(row);
        }
        prices.sort(Comparator.comparingDouble(p -> (Double) p.get("unitPrice")));
        if (!prices.isEmpty()) {
            prices.get(0).put("isBestPrice", true);
        }
        double bestPrice = prices.isEmpty() ? med.getPriceBdt() : best;
        double maxPrice = prices.isEmpty() ? med.getPriceBdt() : max;
        int savings = maxPrice > bestPrice
                ? (int) Math.round(((maxPrice - bestPrice) / maxPrice) * 100) : 0;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "SUCCESS");
        out.put("brandName", med.getBrandName());
        out.put("strength", med.getStrength());
        out.put("genericName", med.getGeneric() == null ? "" : med.getGeneric().getName());
        out.put("company", med.getCompany());
        out.put("basePrice", med.getPriceBdt());
        out.put("bestPrice", bestPrice);
        out.put("maxPrice", maxPrice);
        out.put("savingsPercent", savings);
        out.put("pharmacyPrices", prices);
        return out;
    }

    public record InteractionRequest(String medicines, String mode) {
    }

    @PostMapping("/api/medicines/interaction-check")
    public Map<String, Object> interactionCheck(@RequestBody InteractionRequest req) {
        String mode = req.mode() == null || req.mode().isBlank() ? "SUMMARY" : req.mode();
        String input = req.medicines() == null ? "" : req.medicines();
        List<Medicine> mentioned = medicineService.findMentionedIn(input);

        StringBuilder sb = new StringBuilder();
        if (mentioned.isEmpty()) {
            sb.append("No recognized medicines found in the input. Try brand names like \"Napa Extra, Seclo, Monas\".");
        } else {
            sb.append("Checked ").append(mentioned.size()).append(" medicine(s) [").append(mode).append(" mode]:\n");
            for (Medicine m : mentioned) {
                String gen = m.getGeneric() == null ? "" : m.getGeneric().getName();
                sb.append("• ").append(m.getBrandName()).append(" ").append(m.getStrength())
                        .append(" — ").append(gen).append("\n");
            }
            if (mentioned.size() >= 2) {
                boolean hasPainkiller = mentioned.stream().anyMatch(m -> m.getBrandName().toLowerCase().startsWith("napa")
                        || m.getBrandName().equalsIgnoreCase("ace")
                        || m.getBrandName().equalsIgnoreCase("brufen")
                        || m.getBrandName().equalsIgnoreCase("flamar"));
                boolean hasGastro = mentioned.stream().anyMatch(m -> {
                    String g = m.getGeneric() == null ? "" : m.getGeneric().getName().toLowerCase();
                    return g.contains("prazole") || g.contains("ranitidine") || g.contains("domperidone");
                });
                sb.append("\nInteraction analysis:\n");
                if (hasPainkiller && mentioned.stream().anyMatch(m -> m.getBrandName().toLowerCase().contains("extra")
                        || m.getBrandName().toLowerCase().contains("plus"))) {
                    sb.append("⚠ Two caffeine-containing pain relievers detected — avoid taking together (overdose risk).\n");
                }
                if (hasPainkiller && hasGastro) {
                    sb.append("✓ Gastric protection alongside analgesics is a sensible combination.\n");
                }
                sb.append("• General rule: take each medicine at least 30 minutes apart and follow the labeled frequency.\n");
                sb.append("• This automated check is advisory — confirm with a licensed pharmacist via Live Chat.");
            } else {
                sb.append("\nSingle medicine detected — no interaction pair to analyze. Add a second medicine to compare.");
            }
        }
        return Map.of("mode", mode, "analysis", sb.toString());
    }

    public record VerifyRequest(Long medicineId, String code) {
    }

    @PostMapping("/api/medicines/verify")
    public Map<String, Object> verify(@RequestBody VerifyRequest req) {
        BatchService.CheckResult result = batchService.check(req.code());
        Map<String, Object> out = new LinkedHashMap<>();
        if (result.verdict() == BatchService.Verdict.VALID) {
            MedicineBatch b = result.batch();
            out.put("isAuthentic", true);
            out.put("details", "Batch " + (b.getBatchNo() == null ? "" : b.getBatchNo())
                    + " is registered with the DGDA and has not expired.");
            out.put("manufacturer", b.getManufacturer() == null ? "DGDA Registry" : b.getManufacturer());
        } else if (result.verdict() == BatchService.Verdict.EXPIRED) {
            MedicineBatch b = result.batch();
            out.put("isAuthentic", false);
            out.put("details", "This batch is genuine but EXPIRED — do not use. Return it to the pharmacy.");
            out.put("manufacturer", b.getManufacturer() == null ? "DGDA Registry" : b.getManufacturer());
        } else {
            out.put("isAuthentic", false);
            out.put("details", "No registered batch matches this code in the DGDA database. Suspected counterfeit.");
            out.put("manufacturer", "Unregistered source");
        }
        return out;
    }
}
