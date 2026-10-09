package com.otel.backend.service;

import com.otel.backend.dto.EkHizmetDto;
import com.otel.backend.dto.OdaTipiDto;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.EkHizmetRepository;
import com.otel.backend.repository.OdaTipiRepository;
import com.otel.backend.repository.ProsedurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/** Giriş yapmadan erişilebilen işlemler. */
@Service
@RequiredArgsConstructor
public class GenelService {

    private static final int MAKS_GECE = 30;

    private final OdaTipiRepository odaTipiRepo;
    private final EkHizmetRepository ekHizmetRepo;
    private final ProsedurRepository prosedurRepo;

    @Transactional(readOnly = true)
    public List<OdaTipiDto> odaTipleri() {
        return odaTipiRepo.findAll(Sort.by("temelFiyat")).stream()
                .map(OdaTipiDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EkHizmetDto> ekHizmetler() {
        return ekHizmetRepo.findByAktifTrueOrderByHizmetAdiAsc().stream()
                .map(EkHizmetDto::from)
                .toList();
    }

    /** sp_BosOdalariListele prosedürünü çağırır. */
    public List<Map<String, Object>> bosOdalar(LocalDate giris, LocalDate cikis,
                                               Integer odaTipiId, Integer kisi) {
        if (!cikis.isAfter(giris)) {
            throw ApiHatasi.gecersiz("Çıkış tarihi giriş tarihinden sonra olmalı.");
        }
        if (giris.isBefore(LocalDate.now())) {
            throw ApiHatasi.gecersiz("Giriş tarihi bugünden önce olamaz.");
        }
        if (ChronoUnit.DAYS.between(giris, cikis) > MAKS_GECE) {
            throw ApiHatasi.gecersiz("Tek rezervasyonda en fazla " + MAKS_GECE + " gece seçilebilir.");
        }
        if (kisi != null && (kisi < 1 || kisi > 10)) {
            throw ApiHatasi.gecersiz("Kişi sayısı 1 ile 10 arasında olmalı.");
        }
        return prosedurRepo.bosOdalar(giris, cikis, odaTipiId, kisi);
    }
}