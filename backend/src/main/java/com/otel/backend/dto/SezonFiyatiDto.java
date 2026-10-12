package com.otel.backend.dto;

import com.otel.backend.entity.SezonFiyati;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SezonFiyatiDto(Integer id, Integer odaTipiId, String tipAdi, String sezonAdi,
                             LocalDate baslangicTarihi, LocalDate bitisTarihi, BigDecimal gecelikFiyat) {
    public static SezonFiyatiDto from(SezonFiyati s) {
        return new SezonFiyatiDto(s.getId(), s.getOdaTipi().getId(), s.getOdaTipi().getTipAdi(),
                s.getSezonAdi(), s.getBaslangicTarihi(), s.getBitisTarihi(), s.getGecelikFiyat());
    }
}