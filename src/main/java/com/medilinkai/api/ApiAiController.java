package com.medilinkai.api;

import com.medilinkai.service.MedicineService;
import com.medilinkai.model.Medicine;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 24/7 AI Health Assistant. When the user supplies a Gemini API key the
 * message is forwarded to Google's Generative Language API; without a key
 * (or on any error) a local clinical fallback engine answers.
 */
@RestController
public class ApiAiController {

    private final MedicineService medicineService;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();

    public ApiAiController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    public record StatusRequest(String apiKey) {
    }

    public record ChatTurn(String role, String content) {
    }

    public record ChatRequest(String message, String apiKey, String patientId,
                              List<ChatTurn> history) {
    }

    @PostMapping("/api/ai/status")
    public Map<String, Object> status(@RequestBody StatusRequest req) {
        boolean ok = req.apiKey() != null && req.apiKey().trim().length() > 10;
        return Map.of("status", "SUCCESS", "geminiActive", ok);
    }

    @PostMapping("/api/ai/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest req, HttpSession session) {
        String key = req.apiKey() == null ? null : req.apiKey().trim();
        String message = req.message() == null ? "" : req.message();

        if (key != null && key.length() > 10) {
            try {
                String reply = askGemini(key, message, req.history());
                return Map.of("reply", reply, "provider", "GEMINI_AI");
            } catch (Exception e) {
                // fall through to the local clinical engine
            }
        }
        return Map.of("reply", clinicalFallback(message), "provider", "CLINICAL_FALLBACK");
    }

    private String askGemini(String key, String message, List<ChatTurn> history) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("You are MediLink's clinical health assistant for Bangladesh. ")
                .append("Answer briefly and clearly. If the question needs urgent care, advise seeing a doctor.\n");
        if (history != null) {
            for (ChatTurn t : history) {
                sb.append(t.role()).append(": ").append(t.content()).append('\n');
            }
        }
        sb.append("user: ").append(message);

        String escaped = sb.toString()
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");

        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + escaped + "\"}]}]}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + key))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body();
        int i = json.indexOf("\"text\":");
        if (i < 0) {
            throw new IllegalStateException("no text in response");
        }
        int start = json.indexOf('"', i + 7) + 1;
        int end = json.lastIndexOf('"');
        String text = json.substring(start, Math.max(start, end));
        return text.replace("\\n", "\n").replace("\\\"", "\"").replace("\\/", "/");
    }

    /** Local knowledge-base engine so the assistant always answers. */
    private String clinicalFallback(String message) {
        String q = message.toLowerCase();
        StringBuilder sb = new StringBuilder();

        // try to recognize medicine names from the database
        List<Medicine> mentioned = medicineService.findMentionedIn(message);

        if (q.contains("emergency") || q.contains("urgent") || q.contains("chest pain") || q.contains("breath")) {
            sb.append("🚨 If you have chest pain, severe breathing difficulty, or a suspected allergic reaction, this can be an emergency. ")
                    .append("Use the Emergency tab to find the nearest open pharmacy and contact emergency services immediately.\n\n");
        }
        if (!mentioned.isEmpty()) {
            Medicine m = mentioned.get(0);
            sb.append("**").append(m.getBrandName()).append(" ").append(m.getStrength()).append("** (")
                    .append(m.getGeneric() == null ? "" : m.getGeneric().getName()).append(")\n");
            sb.append("- Used for: ").append(m.getGeneric() == null ? "consult your doctor" : m.getGeneric().getTreats()).append('\n');
            sb.append("- Typical adult dose follows the prescription frequency (e.g. 1+1+1 after meals). Never double a missed dose.\n");
            sb.append("- Approx price: BDT ").append(String.format("%.2f", m.getPriceBdt()))
                    .append(" per unit. Check the Compare Prices card for live pharmacy rates.\n\n");
        }
        if (q.contains("interact") || q.contains("together")) {
            sb.append("For interactions, open the **Check Drug Interactions** tool in the Medicines tab and enter both brand names. ")
                    .append("General rule: separate each medicine by at least 30 minutes and confirm combinations with a pharmacist.\n");
        } else if (q.contains("dose") || q.contains("dosage") || q.contains("how often") || q.contains("how many")) {
            sb.append("Follow the exact frequency printed on your prescription (1+1+1 means morning, afternoon and night after meals). ")
                    .append("Set a reminder in the Reminders tab so you never miss a dose.\n");
        } else if (q.contains("fake") || q.contains("counterfeit") || q.contains("verify") || q.contains("authentic")) {
            sb.append("Use the **Fake Medicine Verifier** tab: type or scan the QR code printed on the pack. ")
                    .append("Codes registered with the DGDA confirm a genuine, unexpired batch; anything unregistered is flagged as suspected counterfeit.\n");
        } else if (q.contains("reminder")) {
            sb.append("Open the **Reminders** tab, click Add Reminder, choose the medicine, dose, time and frequency. ")
                    .append("The scheduler will alert you at the scheduled time.\n");
        } else if (sb.isEmpty()) {
            sb.append("I can help with medicines, dosages, drug interactions, fake-medicine verification, reminders and emergency pharmacy lookup. ")
                    .append("Ask me something like \"What is Napa Extra used for?\" or add your free Gemini API key in Settings to unlock full AI reasoning.\n");
        }
        sb.append("\n_Stay safe — this guidance is informational and does not replace a licensed doctor._");
        return sb.toString();
    }
}
