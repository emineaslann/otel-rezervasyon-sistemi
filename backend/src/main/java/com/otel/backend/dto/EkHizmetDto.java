package com.otel.backend.dto;

import com.otel.backend.entity.EkHizmet;

import java.math.BigDecimal;

public record EkHizmetDto(
        Integer id,
        String hizmetAdi,
        BigDecimal birimFiyat,
        Boolean aktif
) {
    public static EkHizmetDto from(EkHizmet h) {
        return new EkHizmetDto(h.getId(), h.getHizmetAdi(), h.getBirimFiyat(), h.getAktif());
    }
}