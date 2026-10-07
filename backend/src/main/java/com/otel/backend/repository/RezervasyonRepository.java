package com.otel.backend.repository;

import com.otel.backend.entity.Rezervasyon;
import org.springframework.data.jpa.repository.JpaRepository;

/** Yalnızca okuma; yazma işlemleri saklı prosedürlerle (ProsedurRepository). */
public interface RezervasyonRepository extends JpaRepository<Rezervasyon, Integer> {
}