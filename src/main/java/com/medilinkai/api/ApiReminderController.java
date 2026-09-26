package com.medilinkai.api;

import com.medilinkai.model.Reminder;
import com.medilinkai.model.User;
import com.medilinkai.repository.ReminderRepository;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Medicine reminders. The background scheduler is the SSE heartbeat +
 * instant "MEDICINE ALARM" push from the test-alert endpoint.
 */
@RestController
public class ApiReminderController {

    private final ReminderRepository reminderRepo;
    private final UserService userService;
    private final ApiEventBus eventBus;

    public ApiReminderController(ReminderRepository reminderRepo, UserService userService,
                                 ApiEventBus eventBus) {
        this.reminderRepo = reminderRepo;
        this.userService = userService;
        this.eventBus = eventBus;
    }

    private Map<String, Object> dto(Reminder r) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", String.valueOf(r.getId()));
        out.put("medicine", r.getMedicine());
        out.put("dosage", r.getDosage());
        out.put("time", r.getTime());
        out.put("frequency", r.getFrequency());
        out.put("instructions", r.getInstructions() == null ? "" : r.getInstructions());
        out.put("active", r.isActive());
        return out;
    }

    @GetMapping("/api/reminders")
    public Map<String, Object> list(HttpSession session) {
        User me = ApiSession.current(userService, session);
        if (me == null) {
            return Map.of("reminders", List.of());
        }
        List<Reminder> reminders = reminderRepo.findByUserIdOrderByCreatedAtDesc(me.getId());
        return Map.of("reminders", reminders.stream().map(this::dto).collect(Collectors.toList()));
    }

    public record CreateRequest(String email, String medicine, String dosage, String time,
                                String frequency, String instructions) {
    }

    @PostMapping("/api/reminders/create")
    public Map<String, Object> create(@RequestBody CreateRequest req, HttpSession session) {
        User me = ApiSession.current(userService, session);
        if (me == null) {
            return Map.of("status", "ERROR", "message", "Not signed in.");
        }
        Reminder r = new Reminder(me,
                req.medicine() == null ? "Medicine" : req.medicine(),
                req.dosage() == null ? "" : req.dosage(),
                req.time() == null ? "08:00" : req.time(),
                req.frequency() == null ? "1+1+1 (After meal)" : req.frequency(),
                req.instructions());
        reminderRepo.save(r);
        eventBus.publish("REMINDER SET: " + r.getMedicine() + " " + r.getDosage()
                + " scheduled at " + r.getTime() + " for " + me.getEmail());
        return Map.of("status", "SUCCESS");
    }

    @PostMapping("/api/reminders/test-alert")
    public Map<String, Object> testAlert(HttpSession session) {
        User me = ApiSession.current(userService, session);
        String email = me == null ? "" : me.getEmail();
        String time = LocalDateTime.now().toLocalTime().withSecond(0).toString();
        eventBus.publish("MEDICINE ALARM [" + time + "]: Time to take your medicine, "
                + (me == null ? "patient" : me.getName()) + "! (" + email + ")");
        return Map.of("status", "SUCCESS");
    }
}
