package com.otel.backend.dto;

import com.otel.backend.entity.Rol;

/** Kullanıcı hakkında dışarıya gösterilen bilgiler (şifre özeti ASLA yok). */
public record KullaniciBilgisi(
        Integer id,
        String kullaniciAdi,
        Rol rol,
        String adSoyad,
        Integer misafirId,
        Integer personelId
) {}