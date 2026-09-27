package com.medilinkai.config;

import com.medilinkai.model.*;
import com.medilinkai.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills the H2 database with demo data on first run (only when empty).
 * ~25 generics and ~60 brands of real Bangladesh medicines with realistic
 * taka prices, 8 Dhaka pharmacies, demo users and medicine batches.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final GenericRepository genericRepo;
    private final MedicineRepository medicineRepo;
    private final PharmacyRepository pharmacyRepo;
    private final StockRepository stockRepo;
    private final MedicineBatchRepository batchRepo;

    public DataSeeder(UserRepository userRepo, GenericRepository genericRepo,
                      MedicineRepository medicineRepo, PharmacyRepository pharmacyRepo,
                      StockRepository stockRepo, MedicineBatchRepository batchRepo) {
        this.userRepo = userRepo;
        this.genericRepo = genericRepo;
        this.medicineRepo = medicineRepo;
        this.pharmacyRepo = pharmacyRepo;
        this.stockRepo = stockRepo;
        this.batchRepo = batchRepo;
    }

    @Override
    public void run(String... args) {
        // ---------- Already-seeded database: top up newer demo data only ----------
        // NOTE: must key off medicines, not users — the portal demo accounts
        // below create users, which would otherwise short-circuit the main
        // seed on a fresh database.
        if (medicineRepo.count() > 0) {
            ensurePortalDemoAccounts();
            ensureDemoBatches();
            return;
        }

        // ---------- Demo users ----------
        User pharma1 = userRepo.save(new User("Sharmin Akter", "pharma@test.com", "1234", UserRole.PHARMACIST, "01711000002"));
        User pharma2 = userRepo.save(new User("Kamal Hossain", "pharma2@test.com", "1234", UserRole.PHARMACIST, "01711000003"));
        userRepo.save(new User("Rahim Uddin", "patient@test.com", "1234", UserRole.PATIENT, "01711000001"));
        userRepo.save(new User("Ayesha Siddika", "karim@test.com", "1234", UserRole.PATIENT, "01711000009"));
        userRepo.save(new User("Site Admin", "admin@test.com", "1234", UserRole.ADMIN, "01711000004"));

        // ---------- Pharmacies (real Dhaka areas + coordinates) ----------
        List<Pharmacy> pharmacies = List.of(
            pharmacyRepo.save(new Pharmacy("Tamanna Pharmacy", "Dhanmondi", "Road 27, Dhanmondi, Dhaka", 23.7561, 90.3742, "02222271001", true, pharma1)),
            pharmacyRepo.save(new Pharmacy("Lazz Pharma", "Gulshan", "Gulshan Avenue, Gulshan-1, Dhaka", 23.7806, 90.4170, "02222271002", true, pharma2)),
            pharmacyRepo.save(new Pharmacy("Amin Pharmacy", "Mirpur", "Section 10, Mirpur, Dhaka", 23.8067, 90.3686, "02222271003", false, pharma1)),
            pharmacyRepo.save(new Pharmacy("Rahman Drug House", "Uttara", "Sector 7, Uttara, Dhaka", 23.8759, 90.3795, "02222271004", false, pharma2)),
            pharmacyRepo.save(new Pharmacy("New Medical Hall", "Mohammadpur", "Tajmahal Road, Mohammadpur, Dhaka", 23.7658, 90.3588, "02222271005", false, pharma1)),
            pharmacyRepo.save(new Pharmacy("Care Pharmacy", "Banani", "Road 11, Banani, Dhaka", 23.7937, 90.4066, "02222271006", true, pharma2)),
            pharmacyRepo.save(new Pharmacy("Model Pharmacy", "Motijheel", "Motijheel C/A, Dhaka", 23.7330, 90.4172, "02222271007", false, pharma1)),
            pharmacyRepo.save(new Pharmacy("City Drug Point", "Farmgate", "Farmgate, Tejgaon, Dhaka", 23.7580, 90.3896, "02222271008", true, pharma2))
        );

        // ---------- Generics + brands ----------
        // key = generic name, value = [treats] then one line per brand: brand|strength|company|price
        Map<String, String[]> generics = new LinkedHashMap<>();
        generics.put("Paracetamol", new String[]{"Fever and pain",
                "Napa|500mg|Beximco Pharma|2.50",
                "Napa Extra|500mg+65mg|Beximco Pharma|4.00",
                "Ace|500mg|Square Pharma|2.50",
                "Ace Plus|500mg+65mg|Square Pharma|4.20",
                "Reset|500mg|Renata|2.00",
                "Rapid|500mg|ACI|1.80",
                "DP|500mg|Drug International|1.50"});
        generics.put("Ibuprofen", new String[]{"Pain, inflammation and fever",
                "Brufen|400mg|Abbott Bangladesh|3.50",
                "Flamar|400mg|Square Pharma|3.00",
                "Profen|400mg|ACI|2.80"});
        generics.put("Omeprazole", new String[]{"Acidity and gastric ulcer",
                "Seclo|20mg|Square Pharma|7.00",
                "Maxpro|20mg|Renata|6.00",
                "Omez|20mg|Drug International|5.00",
                "Losectil|20mg|Eskayef|6.50"});
        generics.put("Esomeprazole", new String[]{"Severe acidity and reflux",
                "Esso|20mg|Square Pharma|9.00",
                "Esonix|20mg|Healthcare Pharma|8.50"});
        generics.put("Pantoprazole", new String[]{"Acid reflux and stomach pain",
                "Pantonix|20mg|Square Pharma|9.00",
                "Controloc|20mg|Sanofi Bangladesh|12.00"});
        generics.put("Ranitidine", new String[]{"Heartburn and acidity",
                "Neotack|150mg|Square Pharma|5.00",
                "Ranitid|150mg|Beximco Pharma|4.00"});
        generics.put("Domperidone", new String[]{"Nausea and vomiting",
                "Vomistop|10mg|Square Pharma|2.50",
                "Domrid|10mg|Renata|2.20"});
        generics.put("Cetirizine", new String[]{"Allergy, sneezing, itching",
                "Alatrol|10mg|Square Pharma|4.00",
                "Cetriz|10mg|Beximco Pharma|3.50",
                "Riz|10mg|ACI|3.00",
                "Cetisoft|10mg|Sharif Pharma|3.20"});
        generics.put("Fexofenadine", new String[]{"Allergy and hay fever",
                "Fexo|120mg|Square Pharma|9.00",
                "Fexet|120mg|Healthcare Pharma|8.00"});
        generics.put("Montelukast", new String[]{"Asthma prevention and allergy",
                "Montene|10mg|Square Pharma|18.00",
                "Monas|10mg|ACI|16.00"});
        generics.put("Salbutamol", new String[]{"Asthma and breathing difficulty",
                "Ventolin|100mcg inhaler|GSK Bangladesh|320.00",
                "Salbu|100mcg inhaler|ACI|260.00"});
        generics.put("Metformin", new String[]{"Type 2 diabetes",
                "Met|500mg|Square Pharma|3.00",
                "Glucophage|500mg|ACI|4.50",
                "Comet|500mg|Incepta Pharma|3.50"});
        generics.put("Glimepiride", new String[]{"Type 2 diabetes",
                "Getryl|2mg|Square Pharma|8.00",
                "Glimepi|2mg|Beximco Pharma|7.00"});
        generics.put("Insulin Glargine", new String[]{"Diabetes (blood sugar control)",
                "Lantus SoloStar|100IU/ml|Sanofi Bangladesh|1450.00",
                "Basaglar|100IU/ml|Eli Lilly|1200.00"});
        generics.put("Insulin Regular", new String[]{"Diabetes (short-acting insulin)",
                "Humulin R|100IU/ml vial|Eli Lilly|450.00",
                "Insugen R|100IU/ml vial|Biocon|300.00"});
        generics.put("Losartan Potassium", new String[]{"High blood pressure",
                "Losan|50mg|Square Pharma|8.00",
                "Angilock|50mg|Renata|7.00",
                "Losartil|50mg|ACME Labs|6.50"});
        generics.put("Amlodipine", new String[]{"High blood pressure",
                "Amdocal|5mg|Square Pharma|4.50",
                "Amlovas|5mg|ACME Labs|3.80",
                "Amlokind|5mg|Incepta Pharma|4.00"});
        generics.put("Atorvastatin", new String[]{"High cholesterol",
                "Atorva|20mg|Square Pharma|15.00",
                "Lipicard|20mg|Beximco Pharma|13.00"});
        generics.put("Azithromycin", new String[]{"Bacterial infections",
                "Zimax|500mg|Square Pharma|35.00",
                "Azithrocin|500mg|Beximco Pharma|32.00",
                "Azi|500mg|Renata|30.00"});
        generics.put("Amoxicillin", new String[]{"Bacterial infections",
                "Moxacil|500mg|Square Pharma|12.00",
                "Amotid|500mg|Beximco Pharma|11.00",
                "Amoxin|500mg|Renata|10.50"});
        generics.put("Ciprofloxacin", new String[]{"Urinary and gut infections",
                "Ciprocin|500mg|Square Pharma|15.00",
                "Ciprax|500mg|Incepta Pharma|13.00"});
        generics.put("Cefixime", new String[]{"Typhoid and bacterial infections",
                "Maxcef|200mg|Incepta Pharma|28.00",
                "Cef-3|200mg|Healthcare Pharma|30.00"});
        generics.put("ORS (Oral Saline)", new String[]{"Dehydration and diarrhoea",
                "ORSaline|sachet|ACI|4.00",
                "ORSaline-N|sachet|Square Pharma|5.50",
                "Fresaline|sachet|Renata|4.50"});
        generics.put("Calcium + Vitamin D", new String[]{"Bone health, calcium deficiency",
                "Calbo D|tablet|Square Pharma|20.00",
                "Calcin-D|tablet|Beximco Pharma|17.00"});
        generics.put("Iron + Folic Acid", new String[]{"Anaemia, pregnancy nutrition",
                "Fefol|capsule|GSK Bangladesh|15.00",
                "Ferosoft|capsule|Square Pharma|12.00"});

        // save generics and brands
        Map<String, Generic> genericByName = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : generics.entrySet()) {
            Generic g = genericRepo.save(new Generic(e.getKey(), e.getValue()[0]));
            genericByName.put(e.getKey(), g);
            for (int i = 1; i < e.getValue().length; i++) {
                String[] parts = e.getValue()[i].split("\\|");
                medicineRepo.save(new Medicine(parts[0], parts[1], parts[2],
                        Double.parseDouble(parts[3]), g));
            }
        }

        // ---------- Stock: every pharmacy stocks every medicine, varying quantities ----------
        List<Medicine> allMeds = medicineRepo.findAll();
        int n = 0;
        for (Pharmacy p : pharmacies) {
            for (Medicine m : allMeds) {
                int qty = (int) ((p.getId() * 13 + m.getId() * 7 + n) % 60);
                stockRepo.save(new Stock(p, m, qty));
                n++;
            }
        }
        // make sure the Emergency demo works: insulin in stock at Dhanmondi & Farmgate
        setStock(medicineRepo, pharmacies.get(0), "Lantus SoloStar", 25);
        setStock(medicineRepo, pharmacies.get(7), "Lantus SoloStar", 10);
        setStock(medicineRepo, pharmacies.get(0), "Napa", 120);

        // ---------- Medicine batches for fake-medicine detection ----------
        Medicine napa = findMed(medicineRepo, "Napa");
        Medicine lantus = findMed(medicineRepo, "Lantus SoloStar");
        batchRepo.save(new MedicineBatch("QR-NAPA-2026-A1", "BN-8841", "Beximco Pharmaceuticals Ltd.",
                LocalDate.of(2026, 1, 15), LocalDate.of(2028, 1, 14), napa));
        batchRepo.save(new MedicineBatch("QR-LANTUS-2025-B7", "SN-2210", "Sanofi Bangladesh Ltd.",
                LocalDate.of(2025, 6, 1), LocalDate.of(2027, 5, 31), lantus));
        batchRepo.save(new MedicineBatch("QR-NAPA-2023-OLD", "BN-1200", "Beximco Pharmaceuticals Ltd.",
                LocalDate.of(2023, 2, 1), LocalDate.of(2025, 2, 1), napa)); // expired demo

        // portal demo accounts (the "Load demo credentials" buttons)
        ensurePortalDemoAccounts();
    }

    /** Portal demo accounts — idempotent, safe on any database state. */
    private void ensurePortalDemoAccounts() {
        if (userRepo.findByEmail("rahim@medilink.com").isEmpty()) {
            User demoPharma = userRepo.save(new User("Dr. Farhan Kabir", "farhan@lazzpharma.com",
                    "pharma123", UserRole.PHARMACIST, "01711000012"));
            userRepo.save(new User("Rahim Ahmed", "rahim@medilink.com", "patient123", UserRole.PATIENT, "01711000011"));
            userRepo.save(new User("System Administrator", "admin@medilink.com", "admin123", UserRole.ADMIN, "01711000013"));
            pharmacyRepo.save(new Pharmacy("Lazz Pharma Express", "Banani",
                    "Lazz Pharma (Banani) | DGDA-PH-99201", 23.7937, 90.4066, "02222271009", true, demoPharma));
        }
    }

    /** Demo batch codes for fake-medicine detection — idempotent. */
    private void ensureDemoBatches() {
        if (batchRepo.findByQrCodeIgnoreCase("QR-NAPA-2026-A1").isNotEmpty()) {
            return;
        }
        Medicine napaDemo = findMedOrNull(medicineRepo, "Napa");
        Medicine lantusDemo = findMedOrNull(medicineRepo, "Lantus SoloStar");
        if (napaDemo != null && lantusDemo != null) {
            batchRepo.save(new MedicineBatch("QR-NAPA-2026-A1", "BN-8841", "Beximco Pharmaceuticals Ltd.",
                    LocalDate.of(2026, 1, 15), LocalDate.of(2028, 1, 14), napaDemo));
            batchRepo.save(new MedicineBatch("QR-LANTUS-2025-B7", "SN-2210", "Sanofi Bangladesh Ltd.",
                    LocalDate.of(2025, 6, 1), LocalDate.of(2027, 5, 31), lantusDemo));
            batchRepo.save(new MedicineBatch("QR-NAPA-2023-OLD", "BN-1200", "Beximco Pharmaceuticals Ltd.",
                    LocalDate.of(2023, 2, 1), LocalDate.of(2025, 2, 1), napaDemo)); // expired demo
        }
    }

    private static Medicine findMed(MedicineRepository repo, String brandName) {
        return findMedOrNull(repo, brandName);
    }

    private static Medicine findMedOrNull(MedicineRepository repo, String brandName) {
        return repo.findAll().stream()
                .filter(m -> m.getBrandName().equals(brandName))
                .findFirst().orElse(null);
    }

    private void setStock(MedicineRepository medicineRepo, Pharmacy p, String brandName, int qty) {
        Long medId = findMed(medicineRepo, brandName).getId();
        stockRepo.findByPharmacyIdAndMedicineId(p.getId(), medId).ifPresent(s -> {
            s.setQuantity(qty);
            stockRepo.save(s);
        });
    }
}
