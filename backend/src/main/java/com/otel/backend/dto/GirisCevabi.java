package com.otel.backend.dto;

public record GirisCevabi(
        String token,
        String tip,
        long gecerlilikSaniye,
        KullaniciBilgisi kullanici
) {}