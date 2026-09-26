package com.medilinkai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * An uploaded prescription image plus the text we extracted from it.
 */
@Entity
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User patient;

    /** Path of the saved image file on disk */
    private String imagePath;

    @Column(length = 4000)
    private String extractedText;

    /** Workflow state: UPLOADED -> EXTRACTED -> VERIFIED (dispense ready) */
    private String status;

    private String doctorName;

    private String hospital;

    /** Patient voice note stored as a data URL (optional) */
    @Column(length = 2_000_000)
    private String voiceNoteAudio;

    private LocalDateTime uploadedAt;

    public Prescription() {
    }

    public Prescription(User patient, String imagePath, String extractedText) {
        this.patient = patient;
        this.imagePath = imagePath;
        this.extractedText = extractedText;
        this.status = "UPLOADED";
        this.doctorName = "Dr. A. K. Azad (FCPS)";
        this.hospital = "Dhaka Medical College Hospital";
        this.uploadedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getHospital() { return hospital; }
    public void setHospital(String hospital) { this.hospital = hospital; }
    public String getVoiceNoteAudio() { return voiceNoteAudio; }
    public void setVoiceNoteAudio(String voiceNoteAudio) { this.voiceNoteAudio = voiceNoteAudio; }
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
