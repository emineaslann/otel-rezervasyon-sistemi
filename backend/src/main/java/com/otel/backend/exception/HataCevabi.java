package com.otel.backend.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Tüm hatalarda dönen standart JSON gövdesi. */
public record HataCevabi(
        int durum,
        String hata,
        String mesaj,
        Map<String, String> alanlar,   // doğrulama hatalarında hangi alan neden hatalı
        LocalDateTime zaman
) {
    public static HataCevabi of(int durum, String hata, String mesaj) {
        return new HataCevabi(durum, hata, mesaj, null, LocalDateTime.now());
    }
}