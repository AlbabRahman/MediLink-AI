package com.medilinkai.model;

import com.medilinkai.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A scheduled medicine dose reminder for one user. */
@Entity
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private String medicine;

    private String dosage;

    /** Time of day as text, e.g. "08:00" */
    private String time;

    /** e.g. "1+1+1 (After meal)" */
    private String frequency;

    @Column(length = 500)
    private String instructions;

    private boolean active;

    private LocalDateTime createdAt;

    public Reminder() {
    }

    public Reminder(User user, String medicine, String dosage, String time,
                    String frequency, String instructions) {
        this.user = user;
        this.medicine = medicine;
        this.dosage = dosage;
        this.time = time;
        this.frequency = frequency;
        this.instructions = instructions;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getMedicine() { return medicine; }
    public void setMedicine(String medicine) { this.medicine = medicine; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
