package com.otel.backend.dto;

import com.otel.backend.entity.OdaTipi;

import java.math.BigDecimal;

public record OdaTipiDto(
        Integer id,
        String tipAdi,
        Integer kapasite,
        String aciklama,
        BigDecimal temelFiyat
) {
    public static OdaTipiDto from(OdaTipi t) {
        return new OdaTipiDto(t.getId(), t.getTipAdi(), t.getKapasite(), t.getAciklama(), t.getTemelFiyat());
    }
}