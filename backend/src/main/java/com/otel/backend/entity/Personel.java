package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Personel")
@Getter
@Setter
@NoArgsConstructor
public class Personel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PersonelID")
    private Integer id;

    @Column(name = "Ad", nullable = false, length = 50)
    private String ad;

    @Column(name = "Soyad", nullable = false, length = 50)
    private String soyad;

    @Enumerated(EnumType.STRING)
    @Column(name = "Gorev", nullable = false)
    private PersonelGorev gorev;

    @Column(name = "IseGirisTarihi", nullable = false)
    private LocalDate iseGirisTarihi;

    @Column(name = "Telefon", length = 20)
    private String telefon;

    @Column(name = "Aktif", nullable = false)
    private Boolean aktif = true;
}