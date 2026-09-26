package com.medilinkai.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String phone;

    // ---- Extended profile fields used by the patient portal ----
    private String dob;

    private String gender;

    private String bloodType;

    @Column(length = 500)
    private String allergies;

    @Column(length = 500)
    private String chronicConditions;

    /** JSON array of emergency contacts [{name, relationship, phone}] */
    @Column(length = 4000)
    private String emergencyContactsJson;

    /** Custom profile photo as a data URL (uploaded image) */
    @Column(length = 1_000_000)
    private String customAvatar;

    public User() {
    }

    public User(String name, String email, String password, UserRole role, String phone) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getChronicConditions() { return chronicConditions; }
    public void setChronicConditions(String chronicConditions) { this.chronicConditions = chronicConditions; }
    public String getEmergencyContactsJson() { return emergencyContactsJson; }
    public void setEmergencyContactsJson(String emergencyContactsJson) { this.emergencyContactsJson = emergencyContactsJson; }
    public String getCustomAvatar() { return customAvatar; }
    public void setCustomAvatar(String customAvatar) { this.customAvatar = customAvatar; }
}
