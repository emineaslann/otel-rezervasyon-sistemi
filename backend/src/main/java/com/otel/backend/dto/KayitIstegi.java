package com.otel.backend.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record KayitIstegi(
        @NotBlank(message = "Ad boş olamaz.") @Size(max = 50) String ad,
        @NotBlank(message = "Soyad boş olamaz.") @Size(max = 50) String soyad,
        @NotBlank(message = "Kimlik veya pasaport numarası boş olamaz.") @Size(max = 20) String kimlikNo,
        @NotBlank(message = "E-posta boş olamaz.") @Email(message = "Geçerli bir e-posta adresi girin.") String eposta,
        @Size(max = 20) String telefon,
        @Past(message = "Doğum tarihi geçmişte olmalı.") LocalDate dogumTarihi,
        @NotBlank(message = "Şifre boş olamaz.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                 message = "Şifre en az 8 karakter olmalı ve harf ile rakam içermeli.")
        String sifre
) {}