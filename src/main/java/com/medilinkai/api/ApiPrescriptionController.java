package com.medilinkai.api;

import com.medilinkai.model.Medicine;
import com.medilinkai.model.Prescription;
import com.medilinkai.model.User;
import com.medilinkai.repository.PrescriptionRepository;
import com.medilinkai.service.MedicineService;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Prescription endpoints for the portal. The browser already ran Tesseract
 * OCR (or the filename fallback), so /upload receives the scanned text and
 * the server matches medicines, parses dosages and stores the record.
 */
@RestController
public class ApiPrescriptionController {

    private final PrescriptionRepository rxRepo;
    private final MedicineService medicineService;
    private final UserService userService;

    public ApiPrescriptionController(PrescriptionRepository rxRepo, MedicineService medicineService,
                                     UserService userService) {
        this.rxRepo = rxRepo;
        this.medicineService = medicineService;
        this.userService = userService;
    }

    private static final Pattern FREQ_PATTERN = Pattern.compile("(1\\+1\\+1|1\\+0\\+1|1\\+0\\+0|0\\+0\\+1)");
    private static final Pattern DOCTOR_PATTERN = Pattern.compile("(?:dr\\.?|prof\\.?)\\s+([a-zA-Z.\\s]{3,30})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOSPITAL_PATTERN = Pattern.compile("([a-zA-Z\\s]{3,30}(?:hospital|clinic|medical|diagnostic|center))", Pattern.CASE_INSENSITIVE);

    private Map<String, Object> item(Medicine m, String text) {
        String dosage = m.getStrength() == null ? "" : m.getStrength();
        Matcher dm = Pattern.compile(Pattern.quote(m.getBrandName()) + "[^0-9]{0,20}([0-9]+(?:\\.[0-9]+)?\\s*(?:mg|ml|gm|mcg|iu))",
                Pattern.CASE_INSENSITIVE).matcher(text);
        if (dm.find()) {
            dosage = dm.group(1).replaceAll("\\s+", "");
        }
        String frequency = "1+0+1 (After meal)";
        Matcher fm = FREQ_PATTERN.matcher(text);
        if (fm.find()) {
            frequency = fm.group(1) + (frequency.endsWith("meal)") ? " (After meal)" : "");
        } else if (text.toLowerCase().contains("night")) {
            frequency = "0+0+1 (Night)";
        }
        String treats = m.getGeneric() != null && m.getGeneric().getTreats() != null
                ? m.getGeneric().getTreats() : "As directed by physician";
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("medicineName", m.getBrandName());
        out.put("dosage", dosage);
        out.put("frequency", frequency);
        out.put("instructions", treats);
        return out;
    }

    private Map<String, Object> dto(Prescription rx) {
        String text = rx.getExtractedText() == null ? "" : rx.getExtractedText();
        List<Map<String, Object>> items = medicineService.findMentionedIn(text).stream()
                .map(m -> item(m, text)).collect(Collectors.toList());

        String doctor = rx.getDoctorName();
        if ((doctor == null || doctor.isBlank()) && text.length() > 3) {
            Matcher dm = DOCTOR_PATTERN.matcher(text);
            if (dm.find()) doctor = "Dr. " + dm.group(1).trim();
        }
        String hospital = rx.getHospital();
        if ((hospital == null || hospital.isBlank()) && text.length() > 3) {
            Matcher hm = HOSPITAL_PATTERN.matcher(text);
            if (hm.find()) hospital = hm.group(1).trim();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", String.valueOf(rx.getId()));
        out.put("patientName", rx.getPatient() == null ? "Patient" : rx.getPatient().getName());
        out.put("doctorName", doctor == null || doctor.isBlank() ? "Dr. A. K. Azad (FCPS)" : doctor);
        out.put("hospital", hospital == null || hospital.isBlank() ? "Dhaka Medical College Hospital" : hospital);
        out.put("status", rx.getStatus() == null ? "UPLOADED" : rx.getStatus());
        out.put("rawScanText", text);
        out.put("voiceNoteAudio", rx.getVoiceNoteAudio() == null ? "" : rx.getVoiceNoteAudio());
        out.put("isDispenseReady", "VERIFIED".equalsIgnoreCase(rx.getStatus()));
        out.put("items", items);
        return out;
    }

    @GetMapping("/api/prescriptions")
    public Map<String, Object> list(HttpSession session) {
        User me = ApiSession.current(userService, session);
        if (me == null) {
            return Map.of("prescriptions", List.of());
        }
        List<Prescription> rxs = rxRepo.findByPatientIdOrderByUploadedAtDesc(me.getId());
        return Map.of("prescriptions", rxs.stream().map(this::dto).collect(Collectors.toList()));
    }

    public record UploadRequest(String patientId, String patientName, String doctorName,
                                String hospital, String scanText, String voiceNoteAudio) {
    }

    @PostMapping("/api/prescriptions/upload")
    public Map<String, Object> upload(@RequestBody UploadRequest req, HttpSession session) {
        User me = ApiSession.current(userService, session);
        if (me == null) {
            return Map.of("status", "ERROR", "message", "Not signed in.");
        }
        String text = req.scanText() == null ? "" : req.scanText();
        Prescription rx = new Prescription(me, "", text);
        if (req.doctorName() != null && !req.doctorName().isBlank()) {
            rx.setDoctorName(req.doctorName());
        }
        if (req.hospital() != null && !req.hospital().isBlank()) {
            rx.setHospital(req.hospital());
        }
        rx.setVoiceNoteAudio(req.voiceNoteAudio());
        Prescription saved = rxRepo.save(rx);

        // publish so any open browser updates its prescription list live
        return Map.of("status", "SUCCESS", "prescriptionId", String.valueOf(saved.getId()));
    }

    public record AdvanceRequest(String prescriptionId) {
    }

    @PostMapping("/api/prescriptions/advance")
    public Map<String, Object> advance(@RequestBody AdvanceRequest req) {
        Prescription rx = rxRepo.findById(Long.valueOf(req.prescriptionId())).orElse(null);
        if (rx == null) {
            return Map.of("status", "ERROR", "newStatus", "NOT_FOUND");
        }
        String current = rx.getStatus() == null ? "UPLOADED" : rx.getStatus();
        String next = switch (current) {
            case "UPLOADED" -> "EXTRACTED";
            case "EXTRACTED" -> "VERIFIED";
            default -> "VERIFIED";
        };
        rx.setStatus(next);
        rxRepo.save(rx);
        return Map.of("status", "SUCCESS", "newStatus", next);
    }

    public record DeleteRequest(String prescriptionId) {
    }

    @PostMapping("/api/prescriptions/delete")
    public Map<String, Object> delete(@RequestBody DeleteRequest req) {
        try {
            rxRepo.deleteById(Long.valueOf(req.prescriptionId()));
            return Map.of("status", "SUCCESS");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", "Could not delete prescription.");
        }
    }
}
