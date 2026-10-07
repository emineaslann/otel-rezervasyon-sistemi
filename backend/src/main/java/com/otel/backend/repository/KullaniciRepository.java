package com.otel.backend.repository;

import com.otel.backend.entity.Kullanici;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KullaniciRepository extends JpaRepository<Kullanici, Integer> {
    Optional<Kullanici> findByKullaniciAdi(String kullaniciAdi);
    boolean existsByKullaniciAdi(String kullaniciAdi);
}