package com.medilinkai.service;

import com.medilinkai.model.MedicineBatch;
import com.medilinkai.repository.MedicineBatchRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Fake medicine detection: check a QR/barcode from the pack against
 * the reference batch database.
 */
@Service
public class BatchService {

    public enum Verdict { VALID, EXPIRED, NOT_FOUND }

    /** Result of checking one scanned code. */
    public record CheckResult(Verdict verdict, MedicineBatch batch) {
    }

    private final MedicineBatchRepository batchRepo;

    public BatchService(MedicineBatchRepository batchRepo) {
        this.batchRepo = batchRepo;
    }

    public CheckResult check(String code) {
        if (code == null || code.isBlank()) {
            return new CheckResult(Verdict.NOT_FOUND, null);
        }
        Optional<MedicineBatch> found = batchRepo.findByQrCodeIgnoreCase(code.trim());
        if (found.isEmpty()) {
            return new CheckResult(Verdict.NOT_FOUND, null);
        }
        MedicineBatch batch = found.get();
        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now())) {
            return new CheckResult(Verdict.EXPIRED, batch);
        }
        return new CheckResult(Verdict.VALID, batch);
    }
}
