package com.otel.backend.repository;

import com.otel.backend.entity.Personel;
import com.otel.backend.entity.PersonelGorev;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonelRepository extends JpaRepository<Personel, Integer> {
    List<Personel> findByGorevAndAktifTrue(PersonelGorev gorev);
}