package com.otel.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemizlikIstegi(
        @NotNull(message = "Temizliği yapan kat görevlisi seçilmeli.") Integer personelId,
        @Size(max = 255, message = "Not en fazla 255 karakter olabilir.") String notlar
) {}