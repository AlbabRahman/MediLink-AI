package com.medilinkai.controller;

import com.medilinkai.model.Medicine;
import com.medilinkai.model.Prescription;
import com.medilinkai.model.User;
import com.medilinkai.repository.PrescriptionRepository;
import com.medilinkai.service.MedicineService;
import com.medilinkai.service.OcrService;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Feature 1: upload a prescription image, OCR it, match medicines.
 */
@Controller
public class PrescriptionController {

    private final OcrService ocrService;
    private final MedicineService medicineService;
    private final PrescriptionRepository prescriptionRepo;
    private final UserService userService;

    @Value("${medilink.upload-dir}")
    private String uploadDir;

    public PrescriptionController(OcrService ocrService, MedicineService medicineService,
                                  PrescriptionRepository prescriptionRepo, UserService userService) {
        this.ocrService = ocrService;
        this.medicineService = medicineService;
        this.prescriptionRepo = prescriptionRepo;
        this.userService = userService;
    }

    @GetMapping("/prescriptions")
    public String page(HttpSession session, Model model) {
        User me = currentUser(session);
        model.addAttribute("history", prescriptionRepo.findByPatientIdOrderByUploadedAtDesc(me.getId()));
        return "prescription-upload";
    }

    @PostMapping("/prescriptions/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         HttpSession session, Model model) throws IOException {
        User me = currentUser(session);
        if (file.isEmpty()) {
            model.addAttribute("error", "Please choose an image file");
            return "prescription-upload";
        }

        // 1) save the image to disk using IO streams (file handling demo)
        Files.createDirectories(Paths.get(uploadDir));
        String safeName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = Paths.get(uploadDir).resolve(safeName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        // 2) OCR (real Tesseract when installed, simulated fallback otherwise)
        String text = ocrService.extractText(target.toFile(), file.getOriginalFilename());

        // 3) match the extracted text against known medicines and generics
        List<Medicine> matched = medicineService.findMentionedIn(text);

        // 4) store the prescription record
        Prescription saved = prescriptionRepo.save(new Prescription(me, safeName, text));

        model.addAttribute("prescription", saved);
        model.addAttribute("matched", matched);
        model.addAttribute("imageUrl", "/uploads/" + safeName);
        model.addAttribute("history", prescriptionRepo.findByPatientIdOrderByUploadedAtDesc(me.getId()));
        return "prescription-result";
    }

    private User currentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return userService.findById(userId).orElseThrow();
    }
}
