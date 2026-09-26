package com.medilinkai.api;

import com.medilinkai.model.User;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Patient profile settings (personal details, medical ID, emergency contacts, photo). */
@RestController
public class ApiProfileController {

    private final UserService userService;

    public ApiProfileController(UserService userService) {
        this.userService = userService;
    }

    public record ContactRow(String name, String relationship, String phone) {
    }

    public record ProfileRequest(Long id, String name, String email, String dob, String phone,
                                 String gender, String bloodType, String allergies,
                                 String chronicConditions, List<ContactRow> emergencyContacts,
                                 String customAvatar) {
    }

    /** Minimal JSON serializer for the contacts (kept dependency-free). */
    private static String contactsToJson(List<ContactRow> contacts) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < contacts.size(); i++) {
            ContactRow c = contacts.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"name\":\"").append(js(c.name())).append("\",\"relationship\":\"")
                    .append(js(c.relationship())).append("\",\"phone\":\"").append(js(c.phone())).append("\"}");
        }
        return sb.append(']').toString();
    }

    private static String js(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @PostMapping("/api/patient/profile")
    public Map<String, Object> save(@RequestBody ProfileRequest req, HttpSession session) {
        User me = ApiSession.current(userService, session);
        if (me == null) {
            return Map.of("status", "ERROR", "message", "Not signed in.");
        }
        if (req.name() != null && !req.name().isBlank()) {
            me.setName(req.name().trim());
        }
        if (req.email() != null && !req.email().isBlank()) {
            me.setEmail(req.email().trim());
        }
        me.setDob(req.dob());
        me.setPhone(req.phone());
        me.setGender(req.gender());
        me.setBloodType(req.bloodType());
        me.setAllergies(req.allergies());
        me.setChronicConditions(req.chronicConditions());
        if (req.emergencyContacts() != null) {
            List<ContactRow> cleaned = new ArrayList<>();
            for (ContactRow c : req.emergencyContacts()) {
                if ((c.name() != null && !c.name().isBlank()) || (c.phone() != null && !c.phone().isBlank())) {
                    cleaned.add(c);
                }
            }
            me.setEmergencyContactsJson(contactsToJson(cleaned));
        }
        if (req.customAvatar() != null) {
            me.setCustomAvatar(req.customAvatar());
        }
        userService.save(me);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "SUCCESS");
        out.put("name", me.getName());
        out.put("email", me.getEmail());
        out.put("dob", me.getDob());
        out.put("phone", me.getPhone());
        out.put("gender", me.getGender());
        out.put("bloodType", me.getBloodType());
        out.put("allergies", me.getAllergies());
        out.put("chronicConditions", me.getChronicConditions());
        out.put("customAvatar", me.getCustomAvatar());
        return out;
    }
}
