package com.otel.backend.dto;

import jakarta.validation.constraints.*;

public record OdaIstegi(
        @NotBlank(message = "Oda numarası boş olamaz.")
        @Size(max = 10, message = "Oda numarası en fazla 10 karakter olabilir.") String odaNo,
        @NotNull(message = "Kat boş olamaz.")
        @Min(value = -2, message = "Kat en az -2 olabilir.")
        @Max(value = 50, message = "Kat en fazla 50 olabilir.") Integer kat,
        @NotNull(message = "Oda tipi seçilmeli.") Integer odaTipiId
) {}