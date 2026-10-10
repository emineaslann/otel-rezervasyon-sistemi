package com.otel.backend.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Yeni rezervasyon isteği.
 * misafirId ve durum yalnızca resepsiyon/yönetici içindir; misafir kendi adına rezervasyon yapar.
 */
public record RezervasyonIstegi(
        @NotNull(message = "Oda seçilmeli.") Integer odaId,
        @NotNull(message = "Giriş tarihi boş olamaz.")
        @FutureOrPresent(message = "Giriş tarihi bugünden önce olamaz.") LocalDate girisTarihi,
        @NotNull(message = "Çıkış tarihi boş olamaz.") LocalDate cikisTarihi,
        @NotNull(message = "Kişi sayısı boş olamaz.")
        @Min(value = 1, message = "En az 1 kişi olmalı.")
        @Max(value = 10, message = "En fazla 10 kişi olabilir.") Integer kisiSayisi,
        Integer misafirId,
        String durum
) {}