package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Odeme tablosu. Check-out ödemesi sp_CheckOut transaction'ı içinde yazılır;
 * bu entity konaklama sırasındaki ara ödemeler (kapora) için kullanılır.
 */
@Entity
@Table(name = "Odeme")
@Getter
@Setter
@NoArgsConstructor
public class Odeme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OdemeID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "KonaklamaID")
    private Konaklama konaklama;

    @Column(name = "Tutar", nullable = false, precision = 10, scale = 2)
    private BigDecimal tutar;

    @Enumerated(EnumType.STRING)
    @Column(name = "OdemeTuru", nullable = false)
    private OdemeTuru odemeTuru;

    @Column(name = "OdemeTarihi", insertable = false, updatable = false)
    private LocalDateTime odemeTarihi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PersonelID")
    private Personel personel;
}