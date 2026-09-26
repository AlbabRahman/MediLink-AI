package com.medilinkai.repository;

import com.medilinkai.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByPatientIdOrderByUploadedAtDesc(Long patientId);
}
