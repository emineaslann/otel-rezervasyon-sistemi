package com.otel.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HizmetEklemeIstegi(
        @NotNull(message = "Ek hizmet seçilmeli.") Integer ekHizmetId,
        @Min(value = 1, message = "Adet en az 1 olmalı.")
        @Max(value = 100, message = "Adet en fazla 100 olabilir.") Integer adet
) {}