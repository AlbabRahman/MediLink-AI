package com.medilinkai.service;

import com.medilinkai.model.Medicine;
import com.medilinkai.repository.MedicineRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Medicine search and "same generic, different brand" alternative lookup.
 */
@Service
public class MedicineService {

    private final MedicineRepository medicineRepo;

    public MedicineService(MedicineRepository medicineRepo) {
        this.medicineRepo = medicineRepo;
    }

    /** Partial, case-insensitive brand search: "nap" finds Napa, Napa Extra. */
    public List<Medicine> searchByBrand(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return medicineRepo.findByBrandNameContainingIgnoreCase(query.trim());
    }

    /**
     * All brands with the same generic as the given medicine,
     * sorted cheapest first so the patient sees savings immediately.
     */
    public List<Medicine> findAlternatives(Medicine medicine) {
        List<Medicine> sameGeneric = new ArrayList<>(
                medicineRepo.findByGenericId(medicine.getGeneric().getId()));
        sameGeneric.sort(Comparator.comparingDouble(Medicine::getPriceBdt));
        return sameGeneric;
    }

    public List<Medicine> findAll() {
        return medicineRepo.findAll();
    }

    public Medicine findById(Long id) {
        return medicineRepo.findById(id).orElse(null);
    }

    /**
     * Finds every medicine whose brand name or generic name appears in the
     * given text (used to read OCR output from a prescription).
     */
    public List<Medicine> findMentionedIn(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String lower = text.toLowerCase();
        List<Medicine> found = new ArrayList<>();
        for (Medicine m : medicineRepo.findAll()) {
            if (lower.contains(m.getBrandName().toLowerCase())
                    || lower.contains(m.getGeneric().getName().toLowerCase())) {
                found.add(m);
            }
        }
        return found;
    }
}
