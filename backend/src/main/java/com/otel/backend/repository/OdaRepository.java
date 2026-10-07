package com.otel.backend.repository;

import com.otel.backend.entity.Oda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OdaRepository extends JpaRepository<Oda, Integer> {
    Optional<Oda> findByOdaNo(String odaNo);
}