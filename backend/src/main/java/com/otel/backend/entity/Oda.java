package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Oda")
@Getter
@Setter
@NoArgsConstructor
public class Oda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OdaID")
    private Integer id;

    @Column(name = "OdaNo", nullable = false, length = 10)
    private String odaNo;

    @Column(name = "Kat", nullable = false)
    private Integer kat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OdaTipiID")
    private OdaTipi odaTipi;

    @Enumerated(EnumType.STRING)
    @Column(name = "Durum", nullable = false)
    private OdaDurum durum = OdaDurum.BOS;
}