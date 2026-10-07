package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TemizlikKaydi")
@Getter
@Setter
@NoArgsConstructor
public class TemizlikKaydi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TemizlikKaydiID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OdaID")
    private Oda oda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PersonelID")
    private Personel personel;

    @Column(name = "Tarih", insertable = false, updatable = false)
    private LocalDateTime tarih;

    @Enumerated(EnumType.STRING)
    @Column(name = "Durum", nullable = false)
    private TemizlikDurum durum = TemizlikDurum.TAMAMLANDI;

    @Column(name = "Notlar", length = 255)
    private String notlar;
}