package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "OdaTipi")
@Getter
@Setter
@NoArgsConstructor
public class OdaTipi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OdaTipiID")
    private Integer id;

    @Column(name = "TipAdi", nullable = false, length = 50)
    private String tipAdi;

    @Column(name = "Kapasite", nullable = false)
    private Integer kapasite;

    @Column(name = "Aciklama", length = 500)
    private String aciklama;

    @Column(name = "TemelFiyat", nullable = false, precision = 10, scale = 2)
    private BigDecimal temelFiyat;
}