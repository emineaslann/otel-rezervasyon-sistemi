package com.otel.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OdaTipiIstegi(
        @NotBlank(message = "Tip adı boş olamaz.")
        @Size(max = 50, message = "Tip adı en fazla 50 karakter olabilir.") String tipAdi,
        @NotNull(message = "Kapasite boş olamaz.")
        @Min(value = 1, message = "Kapasite en az 1 olmalı.")
        @Max(value = 10, message = "Kapasite en fazla 10 olabilir.") Integer kapasite,
        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir.") String aciklama,
        @NotNull(message = "Temel fiyat boş olamaz.")
        @Positive(message = "Temel fiyat sıfırdan büyük olmalı.") BigDecimal temelFiyat
) {}