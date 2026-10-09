package com.otel.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record GirisIstegi(
        @NotBlank(message = "Kullanıcı adı boş olamaz.") String kullaniciAdi,
        @NotBlank(message = "Şifre boş olamaz.") String sifre
) {}