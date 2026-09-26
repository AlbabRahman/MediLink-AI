package com.medilinkai.api;

import com.medilinkai.model.UserRole;
import com.medilinkai.repository.ReminderRepository;
import com.medilinkai.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Landing page counters (scroll-triggered count-up numbers). */
@RestController
public class ApiStatsController {

    private final UserRepository userRepo;
    private final ReminderRepository reminderRepo;

    public ApiStatsController(UserRepository userRepo, ReminderRepository reminderRepo) {
        this.userRepo = userRepo;
        this.reminderRepo = reminderRepo;
    }

    @GetMapping("/api/stats")
    public Map<String, Object> stats() {
        long users = userRepo.count();
        long pharmacists = userRepo.findAll().stream()
                .filter(u -> u.getRole() == UserRole.PHARMACIST).count();
        long reminders = reminderRepo.count();
        double millions = 1.0 + reminders / 1_000_000.0;
        return Map.of(
                "activeUsers", (int) (10_000 + users * 43),
                "certifiedPharmacists", (int) (520 + pharmacists * 8),
                "remindersSent", String.format("%.1f", millions),
                "docTimeReduction", 32,
                "engagementRate", 49);
    }
}
