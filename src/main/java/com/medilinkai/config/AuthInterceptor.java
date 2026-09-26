package com.medilinkai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Very simple login check: every page needs a logged-in user in the session,
 * except the public paths listed below.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();

        if (isPublic(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return true;
        }

        // API calls get a machine-readable 401 instead of an HTML login redirect
        if (path.startsWith("/api/")) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"ERROR\",\"code\":\"UNAUTHENTICATED\",\"message\":\"Not signed in\"}");
            return false;
        }

        response.sendRedirect("/login");
        return false;
    }

    private boolean isPublic(String path) {
        return path.equals("/")
                || path.equals("/login")
                || path.equals("/register")
                || path.equals("/landing.html")
                || path.equals("/index.html")
                || path.equals("/login.html")
                || path.equals("/dashboard.html")
                || path.equals("/dashboard.js")
                || path.equals("/dashboard.css")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/flags/")
                || path.equals("/style.css")
                || path.equals("/app.js")
                || path.equals("/manifest.json")
                || path.equals("/sw.js")
                || path.startsWith("/icons/")
                || path.startsWith("/api/auth/")
                || path.equals("/api/stats")
                || path.startsWith("/api/events/")
                || path.startsWith("/api/pharmacies/emergency")
                || path.startsWith("/h2-console")
                || path.startsWith("/ws/")
                || path.equals("/error");
    }
}
