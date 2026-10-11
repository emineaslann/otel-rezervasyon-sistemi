package com.otel.backend.dto;

import jakarta.validation.constraints.NotNull;

public record CheckInIstegi(
        @NotNull(message = "Rezervasyon seçilmeli.") Integer rezervasyonId
) {}