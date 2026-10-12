package com.otel.backend.repository;

import com.otel.backend.entity.SezonFiyati;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SezonFiyatiRepository extends JpaRepository<SezonFiyati, Integer> {
    /** odaTipi.id alanına göre filtrele, başlangıç tarihine göre sırala */
    List<SezonFiyati> findByOdaTipi_IdOrderByBaslangicTarihiAsc(Integer odaTipiId);
}