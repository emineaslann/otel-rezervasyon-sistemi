package com.otel.backend.dto;

import com.otel.backend.entity.OdaDurum;
import jakarta.validation.constraints.NotNull;

public record OdaDurumIstegi(
        @NotNull(message = "Yeni durum seçilmeli.") OdaDurum durum
) {}