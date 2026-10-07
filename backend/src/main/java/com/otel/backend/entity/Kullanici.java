package com.otel.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Kullanici")
@Getter
@Setter
@NoArgsConstructor
public class Kullanici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KullaniciID")
    private Integer id;

    @Column(name = "KullaniciAdi", nullable = false, length = 120)
    private String kullaniciAdi;

    @Column(name = "SifreHash", nullable = false)
    private String sifreHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "Rol", nullable = false)
    private Rol rol;

    // Kullanıcı ya bir misafire ya bir personele bağlıdır (veritabanındaki CHECK kısıtı)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MisafirID")
    private Misafir misafir;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PersonelID")
    private Personel personel;

    @Column(name = "Aktif", nullable = false)
    private Boolean aktif = true;

    @Column(name = "OlusturmaTarihi", insertable = false, updatable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "SonGiris")
    private LocalDateTime sonGiris;
}