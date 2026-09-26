package com.medilinkai.config;

import com.medilinkai.socket.SessionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket session registries live in their own configuration class so that
 * the handlers (which inject these beans) and WebSocketConfig (which injects
 * the handlers) do not form a circular bean dependency.
 */
@Configuration
public class RegistryConfig {

    /** Chat sessions live here — separate from stock sessions. */
    @Bean
    public SessionRegistry chatRegistry() {
        return new SessionRegistry();
    }

    /** Stock-push sessions live here — separate from chat sessions. */
    @Bean
    public SessionRegistry stockRegistry() {
        return new SessionRegistry();
    }
}
