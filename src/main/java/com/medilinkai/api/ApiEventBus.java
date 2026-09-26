package com.medilinkai.api;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events bus for the static portal (landing/index pages).
 * The frontend opens GET /api/events/stream; every publish() call reaches
 * all connected browsers as a plain text message (same wire format the
 * reference site expects, e.g. "STOCK_UPDATE: Napa 500mg now 55 units").
 */
@Component
public class ApiEventBus {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    /** Push one text message to every connected browser. Safe to call from anywhere. */
    public void publish(String message) {
        for (SseEmitter emitter : emitters) {
            try {
                // plain text payload — the portal reads event.data directly
                emitter.send(SseEmitter.event().data(message));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public int subscriberCount() {
        return emitters.size();
    }
}
