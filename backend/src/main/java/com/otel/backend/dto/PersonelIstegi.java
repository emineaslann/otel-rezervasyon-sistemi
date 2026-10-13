package com.otel.backend.dto;

import com.otel.backend.entity.PersonelGorev;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PersonelIstegi(
        @NotBlank(message = "Ad boş olamaz.") @Size(max = 50) String ad,
        @NotBlank(message = "Soyad boş olamaz.") @Size(max = 50) String soyad,
        @NotNull(message = "Görev seçilmeli (RESEPSIYON, KAT_GOREVLISI, YONETICI).") PersonelGorev gorev,
        @NotNull(message = "İşe giriş tarihi boş olamaz.")
        @PastOrPresent(message = "İşe giriş tarihi gelecekte olamaz.") LocalDate iseGirisTarihi,
        @Size(max = 20) String telefon,
        Boolean aktif
) {}