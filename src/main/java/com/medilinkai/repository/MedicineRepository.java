package com.medilinkai.repository;

import com.medilinkai.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    List<Medicine> findByBrandNameContainingIgnoreCase(String name);

    List<Medicine> findByGenericId(Long genericId);
}
