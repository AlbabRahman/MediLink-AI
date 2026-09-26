package com.medilinkai.repository;

import com.medilinkai.model.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    List<Pharmacy> findByOwnerId(Long ownerId);
}
