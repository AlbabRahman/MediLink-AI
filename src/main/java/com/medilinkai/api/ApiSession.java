package com.medilinkai.api;

import com.medilinkai.model.User;
import com.medilinkai.repository.UserRepository;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

/**
 * Shared helper for API controllers: resolve the logged-in portal user
 * from the HTTP session (nullable — some endpoints are public).
 */
public final class ApiSession {

    private ApiSession() {
    }

    /** Current session user or null (public/anonymous request). */
    public static User current(UserService userService, HttpSession session) {
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute("userId");
        if (id instanceof Long l) {
            return userService.findById(l).orElse(null);
        }
        return null;
    }
}
