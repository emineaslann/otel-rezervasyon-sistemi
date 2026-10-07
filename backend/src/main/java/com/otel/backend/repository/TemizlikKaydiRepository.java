package com.otel.backend.repository;

import com.otel.backend.entity.TemizlikKaydi;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemizlikKaydiRepository extends JpaRepository<TemizlikKaydi, Integer> {
}