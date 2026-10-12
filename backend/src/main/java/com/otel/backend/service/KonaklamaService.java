package com.otel.backend.service;

import com.otel.backend.dto.HizmetEklemeIstegi;
import com.otel.backend.dto.OdemeIstegi;
import com.otel.backend.entity.*;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.*;
import com.otel.backend.security.OturumKullanicisi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Konaklama sırasındaki işlemler: ek hizmet ve ara ödeme. */
@Service
@RequiredArgsConstructor
public class KonaklamaService {

    private final KonaklamaRepository konaklamaRepo;
    private final EkHizmetRepository ekHizmetRepo;
    private final KonaklamaHizmetiRepository konaklamaHizmetiRepo;
    private final OdemeRepository odemeRepo;
    private final PersonelRepository personelRepo;
    private final ProsedurRepository prosedurRepo;

    @Transactional
    public Map<String, Object> hizmetEkle(int konaklamaId, HizmetEklemeIstegi istek) {
        Konaklama konaklama = acikKonaklama(konaklamaId);

        EkHizmet hizmet = ekHizmetRepo.findById(istek.ekHizmetId())
                .filter(EkHizmet::getAktif)
                .orElseThrow(() -> ApiHatasi.gecersiz("Seçilen ek hizmet bulunamadı veya satışta değil."));

        int adet = (istek.adet() == null) ? 1 : istek.adet();

        KonaklamaHizmeti kayit = new KonaklamaHizmeti();
        kayit.setKonaklama(konaklama);
        kayit.setEkHizmet(hizmet);
        kayit.setAdet(adet);
        kayit.setBirimFiyat(hizmet.getBirimFiyat());   // satış anındaki fiyat saklanır
        konaklamaHizmetiRepo.save(kayit);

        Map<String, Object> cevap = new LinkedHashMap<>();
        cevap.put("konaklamaHizmetiId", kayit.getId());
        cevap.put("hizmetAdi", hizmet.getHizmetAdi());
        cevap.put("adet", adet);
        cevap.put("birimFiyat", hizmet.getBirimFiyat());
        cevap.put("tutar", hizmet.getBirimFiyat().multiply(BigDecimal.valueOf(adet)));
        return cevap;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> odemeAl(OturumKullanicisi ben, int konaklamaId, OdemeIstegi istek) {
        Konaklama konaklama = acikKonaklama(konaklamaId);

        // Kalan bakiyeyi prosedürden öğren: fazla ödeme alınmasın
        Map<String, Object> ozet = (Map<String, Object>)
                prosedurRepo.konaklamaToplamTutar(konaklamaId).get("ozet");
        BigDecimal kalan = new BigDecimal(ozet.get("kalan").toString());
        if (istek.tutar().compareTo(kalan) > 0) {
            throw ApiHatasi.gecersiz("Tutar kalan bakiyeden (" + kalan + " TL) fazla olamaz.");
        }

        Odeme odeme = new Odeme();
        odeme.setKonaklama(konaklama);
        odeme.setTutar(istek.tutar());
        odeme.setOdemeTuru(istek.odemeTuru());
        if (ben.personelId() != null) {
            odeme.setPersonel(personelRepo.getReferenceById(ben.personelId()));
        }
        odemeRepo.save(odeme);    // trg_Odeme_Log_AI -> IslemLog

        Map<String, Object> cevap = new LinkedHashMap<>();
        cevap.put("odemeId", odeme.getId());
        cevap.put("tutar", odeme.getTutar());
        cevap.put("odemeTuru", odeme.getOdemeTuru());
        cevap.put("kalanBakiye", kalan.subtract(istek.tutar()));
        return cevap;
    }

    /** Konaklama var mı ve hâlâ açık mı? */
    private Konaklama acikKonaklama(int konaklamaId) {
        Konaklama k = konaklamaRepo.findById(konaklamaId)
                .orElseThrow(() -> ApiHatasi.bulunamadi("Konaklama bulunamadı."));
        if (!k.isAcik()) {
            throw ApiHatasi.cakisma("Bu konaklama kapanmış (check-out yapılmış); işlem eklenemez.");
        }
        return k;
    }
}