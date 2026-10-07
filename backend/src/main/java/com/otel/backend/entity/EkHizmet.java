package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "EkHizmet")
@Getter
@Setter
@NoArgsConstructor
public class EkHizmet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EkHizmetID")
    private Integer id;

    @Column(name = "HizmetAdi", nullable = false, length = 80)
    private String hizmetAdi;

    @Column(name = "BirimFiyat", nullable = false, precision = 10, scale = 2)
    private BigDecimal birimFiyat;

    @Column(name = "Aktif", nullable = false)
    private Boolean aktif = true;
}