package com.otel.backend.repository;

import com.otel.backend.entity.Odeme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OdemeRepository extends JpaRepository<Odeme, Integer> {
}