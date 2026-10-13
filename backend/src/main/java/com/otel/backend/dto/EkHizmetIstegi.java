package com.otel.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record EkHizmetIstegi(
        @NotBlank(message = "Hizmet adı boş olamaz.")
        @Size(max = 80, message = "Hizmet adı en fazla 80 karakter olabilir.") String hizmetAdi,
        @NotNull(message = "Birim fiyat boş olamaz.")
        @PositiveOrZero(message = "Birim fiyat negatif olamaz.") BigDecimal birimFiyat,
        Boolean aktif
) {}