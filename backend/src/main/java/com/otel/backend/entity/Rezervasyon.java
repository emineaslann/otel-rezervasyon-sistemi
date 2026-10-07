package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rezervasyon tablosu.
 * YAZMA işlemleri saklı prosedürlerle yapılır (sp_RezervasyonOlustur, sp_RezervasyonIptal,
 * sp_RezervasyonOnayla); bu entity okuma ve sahiplik kontrolleri içindir.
 */
@Entity
@Table(name = "Rezervasyon")
@Getter
@Setter
@NoArgsConstructor
public class Rezervasyon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RezervasyonID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MisafirID")
    private Misafir misafir;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OdaID")
    private Oda oda;

    @Column(name = "GirisTarihi", nullable = false)
    private LocalDate girisTarihi;

    @Column(name = "CikisTarihi", nullable = false)
    private LocalDate cikisTarihi;

    @Column(name = "KisiSayisi", nullable = false)
    private Integer kisiSayisi;

    @Enumerated(EnumType.STRING)
    @Column(name = "Durum", nullable = false)
    private RezervasyonDurum durum;

    @Column(name = "OlusturmaTarihi", insertable = false, updatable = false)
    private LocalDateTime olusturmaTarihi;
}