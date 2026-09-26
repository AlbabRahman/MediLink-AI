package com.medilinkai.repository;

import com.medilinkai.model.Generic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenericRepository extends JpaRepository<Generic, Long> {

    Optional<Generic> findByNameIgnoreCase(String name);
}
