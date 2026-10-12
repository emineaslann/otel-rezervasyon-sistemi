package com.otel.backend.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Resepsiyonda kapıdan gelen misafir kaydı (sistem hesabı açılmaz). */
public record MisafirIstegi(
        @NotBlank(message = "Ad boş olamaz.") @Size(max = 50) String ad,
        @NotBlank(message = "Soyad boş olamaz.") @Size(max = 50) String soyad,
        @NotBlank(message = "Kimlik veya pasaport numarası boş olamaz.") @Size(max = 20) String kimlikNo,
        @NotBlank(message = "E-posta boş olamaz.") @Email(message = "Geçerli bir e-posta adresi girin.") String eposta,
        @Size(max = 20) String telefon,
        @Past(message = "Doğum tarihi geçmişte olmalı.") LocalDate dogumTarihi
) {}
