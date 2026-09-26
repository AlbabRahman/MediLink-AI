package com.medilinkai.repository;

import com.medilinkai.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findByPharmacyId(Long pharmacyId);

    List<Stock> findByMedicineIdAndQuantityGreaterThan(Long medicineId, int minQuantity);

    Optional<Stock> findByPharmacyIdAndMedicineId(Long pharmacyId, Long medicineId);
}
