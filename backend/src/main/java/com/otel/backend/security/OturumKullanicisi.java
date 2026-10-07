package com.otel.backend.security;

import com.otel.backend.entity.Rol;

/**
 * Token'dan okunan, isteği yapan kullanıcının bilgileri.
 * Controller'larda @AuthenticationPrincipal ile alınır.
 */
public record OturumKullanicisi(
        Integer kullaniciId,
        String kullaniciAdi,
        Rol rol,
        Integer misafirId,
        Integer personelId
) {
    public boolean misafirMi() {
        return rol == Rol.MISAFIR;
    }
}