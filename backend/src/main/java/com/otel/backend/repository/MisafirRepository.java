package com.otel.backend.repository;

import com.otel.backend.entity.Misafir;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MisafirRepository extends JpaRepository<Misafir, Integer> {
    boolean existsByEposta(String eposta);
    boolean existsByKimlikNo(String kimlikNo);
}