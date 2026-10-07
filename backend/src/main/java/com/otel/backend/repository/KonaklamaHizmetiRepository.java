package com.otel.backend.repository;

import com.otel.backend.entity.KonaklamaHizmeti;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KonaklamaHizmetiRepository extends JpaRepository<KonaklamaHizmeti, Integer> {
}