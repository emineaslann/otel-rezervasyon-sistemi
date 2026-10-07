package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Konaklama <-> EkHizmet arasındaki N-N ilişkinin ara tablosu. */
@Entity
@Table(name = "KonaklamaHizmeti")
@Getter
@Setter
@NoArgsConstructor
public class KonaklamaHizmeti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KonaklamaHizmetiID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "KonaklamaID")
    private Konaklama konaklama;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EkHizmetID")
    private EkHizmet ekHizmet;

    @Column(name = "Adet", nullable = false)
    private Integer adet = 1;

    /** Satış anındaki fiyat (EkHizmet fiyatı sonradan değişse de bu değişmez). */
    @Column(name = "BirimFiyat", nullable = false, precision = 10, scale = 2)
    private BigDecimal birimFiyat;

    @Column(name = "Tarih", insertable = false, updatable = false)
    private LocalDateTime tarih;
}