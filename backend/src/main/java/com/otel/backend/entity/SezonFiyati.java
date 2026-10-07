package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "SezonFiyati")
@Getter
@Setter
@NoArgsConstructor
public class SezonFiyati {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SezonFiyatiID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OdaTipiID")
    private OdaTipi odaTipi;

    @Column(name = "SezonAdi", nullable = false, length = 50)
    private String sezonAdi;

    @Column(name = "BaslangicTarihi", nullable = false)
    private LocalDate baslangicTarihi;

    @Column(name = "BitisTarihi", nullable = false)
    private LocalDate bitisTarihi;

    @Column(name = "GecelikFiyat", nullable = false, precision = 10, scale = 2)
    private BigDecimal gecelikFiyat;
}