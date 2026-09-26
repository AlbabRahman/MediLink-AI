package com.medilinkai.controller;

import com.medilinkai.model.User;
import com.medilinkai.model.UserRole;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** Landing page: send each role to its home screen. */
    @GetMapping("/")
    public String root(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/landing.html";
        }
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return "redirect:/landing.html";
        }
        return "redirect:/index.html";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email, @RequestParam String password,
                          HttpSession session, Model model) {
        Optional<User> user = userService.login(email, password);
        if (user.isEmpty()) {
            model.addAttribute("error", "Wrong email or password");
            return "login";
        }
        session.setAttribute("userId", user.get().getId());
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String name, @RequestParam String email,
                             @RequestParam String password, @RequestParam String phone,
                             HttpSession session, Model model) {
        try {
            User user = userService.register(name, email, password, phone);
            session.setAttribute("userId", user.getId());
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "This email is already registered");
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /** Patient home page with links to all features. */
    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = currentUser(session);
        model.addAttribute("user", user);
        return "home";
    }

    private User currentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return userId == null ? null : userService.findById(userId).orElse(null);
    }
}
