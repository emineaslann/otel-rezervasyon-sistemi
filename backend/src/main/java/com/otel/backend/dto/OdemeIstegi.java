package com.otel.backend.dto;

import com.otel.backend.entity.OdemeTuru;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OdemeIstegi(
        @NotNull(message = "Tutar boş olamaz.")
        @Positive(message = "Tutar sıfırdan büyük olmalı.") BigDecimal tutar,
        @NotNull(message = "Ödeme türü seçilmeli (NAKIT veya KART).") OdemeTuru odemeTuru
) {}