package com.medilinkai.service;

import com.medilinkai.model.Pharmacy;
import com.medilinkai.model.User;
import com.medilinkai.model.UserRole;
import com.medilinkai.repository.PharmacyRepository;
import com.medilinkai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Simple login/register logic.
 * NOTE: passwords are stored in plain text because this is a university demo.
 * A real system must hash passwords (e.g. BCrypt).
 */
@Service
public class UserService {

    private final UserRepository userRepo;
    private PharmacyRepository pharmacyRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /** Returns the user when email+password match, otherwise empty. */
    public Optional<User> login(String email, String password) {
        Optional<User> user = userRepo.findByEmail(email);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user;
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public User save(User user) {
        return userRepo.save(user);
    }

    public User register(String name, String email, String password, String phone) {
        User user = new User(name, email, password, UserRole.PATIENT, phone);
        return userRepo.save(user);
    }

    /** Portal registration: supports all three roles. Pharmacists also get a pharmacy. */
    public User registerWithRole(String name, String email, String password, String role, String extra) {
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role == null ? "PATIENT" : role.toUpperCase());
        } catch (IllegalArgumentException e) {
            userRole = UserRole.PATIENT;
        }
        User user = new User(name, email, password, userRole, "");
        User saved = userRepo.save(user);

        if (userRole == UserRole.PHARMACIST && pharmacyRepo != null && extra != null && !extra.isBlank()) {
            // extra looks like "Lazz Pharma (Dhanmondi) | DGDA-PH-99201"
            String pharmacyName = extra.split("\\|")[0].trim();
            if (pharmacyName.isBlank()) pharmacyName = name + "'s Pharmacy";
            pharmacyRepo.save(new Pharmacy(pharmacyName, "Dhaka", extra, 23.7806, 90.4074,
                    "", false, saved));
        }
        return saved;
    }

    @Autowired(required = false)
    public void setPharmacyRepo(PharmacyRepository pharmacyRepo) {
        this.pharmacyRepo = pharmacyRepo;
    }

    public Optional<User> findById(Long id) {
        return userRepo.findById(id);
    }

    public List<User> findByRole(UserRole role) {
        return userRepo.findByRole(role);
    }

    public List<User> findAll() {
        return userRepo.findAll();
    }
}
