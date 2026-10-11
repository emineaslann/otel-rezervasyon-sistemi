package com.otel.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * tutar boş bırakılırsa kalan bakiyenin tamamı tahsil edilir.
 * odemeTuru bilinçli olarak Java'da doğrulanmıyor: geçersiz bir değer veritabanındaki
 * ENUM kısıtına takılır ve transaction'ın geri alındığı (ROLLBACK) gösterilebilir.
 */
public record CheckOutIstegi(
        @NotNull(message = "Konaklama seçilmeli.") Integer konaklamaId,
        @NotBlank(message = "Ödeme türü seçilmeli.") String odemeTuru,
        BigDecimal tutar
) {}
