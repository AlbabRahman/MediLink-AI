package com.medilinkai.repository;

import com.medilinkai.model.MedicineBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, Long> {

    Optional<MedicineBatch> findByQrCodeIgnoreCase(String qrCode);
}
