package com.otel.backend.service;

import com.otel.backend.dto.RezervasyonIstegi;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.GorunumRepository;
import com.otel.backend.repository.ProsedurRepository;
import com.otel.backend.security.OturumKullanicisi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RezervasyonService {

    private static final Set<String> YENI_DURUMLAR = Set.of("BEKLEMEDE", "ONAYLI");

    private final ProsedurRepository prosedurRepo;
    private final GorunumRepository gorunumRepo;

    /**
     * Misafir: kendi adına, durum BEKLEMEDE (resepsiyon onayına düşer).
     * Resepsiyon/Yönetici: misafirId zorunlu, varsayılan durum ONAYLI (telefonla gelen rezervasyon).
     * Çakışma ve kapasite kontrolünü veritabanındaki trigger yapar.
     */
    public Map<String, Object> olustur(OturumKullanicisi ben, RezervasyonIstegi r) {
        if (!r.cikisTarihi().isAfter(r.girisTarihi())) {
            throw ApiHatasi.gecersiz("Çıkış tarihi giriş tarihinden sonra olmalı.");
        }

        int misafirId;
        String durum;
        if (ben.misafirMi()) {
            misafirId = ben.misafirId();
            durum = "BEKLEMEDE";
        } else {
            if (r.misafirId() == null) {
                throw ApiHatasi.gecersiz("Resepsiyon rezervasyonunda misafir seçilmeli (misafirId).");
            }
            misafirId = r.misafirId();
            durum = (r.durum() == null) ? "ONAYLI" : r.durum();
            if (!YENI_DURUMLAR.contains(durum)) {
                throw ApiHatasi.gecersiz("Yeni rezervasyonun durumu BEKLEMEDE veya ONAYLI olabilir.");
            }
        }

        return prosedurRepo.rezervasyonOlustur(misafirId, r.odaId(), r.girisTarihi(),
                r.cikisTarihi(), r.kisiSayisi(), durum);
    }

    public List<Map<String, Object>> benim(OturumKullanicisi ben) {
        return gorunumRepo.misafirRezervasyonlari(ben.misafirId());
    }

    public List<Map<String, Object>> gecmis(OturumKullanicisi ben) {
        return gorunumRepo.misafirGecmisi(ben.misafirId());
    }

    /** Misafir yalnızca kendi rezervasyonunu iptal edebilir (kontrol prosedürde). */
    public Map<String, Object> iptal(OturumKullanicisi ben, int rezervasyonId) {
        Integer sahiplikKontrolu = ben.misafirMi() ? ben.misafirId() : null;
        return prosedurRepo.rezervasyonIptal(rezervasyonId, sahiplikKontrolu);
    }
}