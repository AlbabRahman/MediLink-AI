package com.medilinkai.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Server-Sent Events stream consumed by the portal (observer pattern). */
@RestController
public class ApiEventStreamController {

    private final ApiEventBus eventBus;
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    public ApiEventStreamController(ApiEventBus eventBus) {
        this.eventBus = eventBus;
        heartbeat.scheduleAtFixedRate(() -> eventBus.publish("HEARTBEAT " + System.currentTimeMillis()),
                25, 25, TimeUnit.SECONDS);
    }

    @GetMapping(path = "/api/events/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        return eventBus.subscribe();
    }
}
