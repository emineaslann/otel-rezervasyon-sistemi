package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Konaklama tablosu (check-in / check-out).
 * Açma ve kapatma sp_CheckIn ve sp_CheckOut prosedürleriyle yapılır.
 */
@Entity
@Table(name = "Konaklama")
@Getter
@Setter
@NoArgsConstructor
public class Konaklama {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KonaklamaID")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RezervasyonID")
    private Rezervasyon rezervasyon;

    @Column(name = "GercekGiris", nullable = false)
    private LocalDateTime gercekGiris;

    @Column(name = "GercekCikis")
    private LocalDateTime gercekCikis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "GirisPersonelID")
    private Personel girisPersonel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CikisPersonelID")
    private Personel cikisPersonel;

    /** Çıkış zamanı boşsa misafir hâlâ oteldedir. */
    public boolean isAcik() {
        return gercekCikis == null;
    }
}