package com.otel.backend.dto;

import com.otel.backend.entity.Personel;
import com.otel.backend.entity.PersonelGorev;

import java.time.LocalDate;

public record PersonelDto(Integer id, String ad, String soyad, PersonelGorev gorev,
                          LocalDate iseGirisTarihi, String telefon, Boolean aktif) {
    public static PersonelDto from(Personel p) {
        return new PersonelDto(p.getId(), p.getAd(), p.getSoyad(), p.getGorev(),
                p.getIseGirisTarihi(), p.getTelefon(), p.getAktif());
    }
}