package com.otel.backend.repository;

import com.otel.backend.entity.Konaklama;
import org.springframework.data.jpa.repository.JpaRepository;

/** Yalnızca okuma; check-in/check-out saklı prosedürlerle (ProsedurRepository). */
public interface KonaklamaRepository extends JpaRepository<Konaklama, Integer> {
}