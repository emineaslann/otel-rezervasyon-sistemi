package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Misafir")
@Getter
@Setter
@NoArgsConstructor
public class Misafir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MisafirID")
    private Integer id;

    @Column(name = "Ad", nullable = false, length = 50)
    private String ad;

    @Column(name = "Soyad", nullable = false, length = 50)
    private String soyad;

    @Column(name = "KimlikNo", nullable = false, length = 20)
    private String kimlikNo;

    @Column(name = "Telefon", length = 20)
    private String telefon;

    @Column(name = "Eposta", nullable = false, length = 120)
    private String eposta;

    @Column(name = "DogumTarihi")
    private LocalDate dogumTarihi;

    // Değeri veritabanı (DEFAULT CURRENT_TIMESTAMP) verir; Java yazmaz
    @Column(name = "KayitTarihi", insertable = false, updatable = false)
    private LocalDateTime kayitTarihi;
}