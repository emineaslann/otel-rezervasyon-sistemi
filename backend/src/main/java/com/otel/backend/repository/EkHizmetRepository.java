package com.otel.backend.repository;

import com.otel.backend.entity.EkHizmet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EkHizmetRepository extends JpaRepository<EkHizmet, Integer> {
    List<EkHizmet> findByAktifTrueOrderByHizmetAdiAsc();
}