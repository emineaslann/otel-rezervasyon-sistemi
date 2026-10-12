package com.otel.backend.dto;

import com.otel.backend.entity.Oda;
import com.otel.backend.entity.OdaDurum;

public record OdaDto(Integer id, String odaNo, Integer kat, Integer odaTipiId, String tipAdi, OdaDurum durum) {
    public static OdaDto from(Oda o) {
        return new OdaDto(o.getId(), o.getOdaNo(), o.getKat(),
                o.getOdaTipi().getId(), o.getOdaTipi().getTipAdi(), o.getDurum());
    }
}