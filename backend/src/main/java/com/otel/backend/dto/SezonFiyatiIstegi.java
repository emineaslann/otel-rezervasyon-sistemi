package com.otel.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SezonFiyatiIstegi(
        @NotNull(message = "Oda tipi seçilmeli.") Integer odaTipiId,
        @NotBlank(message = "Sezon adı boş olamaz.")
        @Size(max = 50, message = "Sezon adı en fazla 50 karakter olabilir.") String sezonAdi,
        @NotNull(message = "Başlangıç tarihi boş olamaz.") LocalDate baslangicTarihi,
        @NotNull(message = "Bitiş tarihi boş olamaz.") LocalDate bitisTarihi,
        @NotNull(message = "Gecelik fiyat boş olamaz.")
        @Positive(message = "Gecelik fiyat sıfırdan büyük olmalı.") BigDecimal gecelikFiyat
) {}