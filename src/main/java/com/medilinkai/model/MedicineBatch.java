package com.medilinkai.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * One manufactured batch of a medicine. The QR/barcode on the box maps here,
 * so patients can check the pack is genuine and not expired.
 */
@Entity
public class MedicineBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The code printed on the box (QR / barcode content) */
    @Column(unique = true, nullable = false)
    private String qrCode;

    private String batchNo;

    private String manufacturer;

    private LocalDate mfgDate;

    private LocalDate expiryDate;

    @ManyToOne
    private Medicine medicine;

    public MedicineBatch() {
    }

    public MedicineBatch(String qrCode, String batchNo, String manufacturer,
                         LocalDate mfgDate, LocalDate expiryDate, Medicine medicine) {
        this.qrCode = qrCode;
        this.batchNo = batchNo;
        this.manufacturer = manufacturer;
        this.mfgDate = mfgDate;
        this.expiryDate = expiryDate;
        this.medicine = medicine;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public LocalDate getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(LocalDate mfgDate) {
        this.mfgDate = mfgDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }
}
