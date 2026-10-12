package com.otel.backend.service;

import com.otel.backend.dto.CheckInIstegi;
import com.otel.backend.dto.CheckOutIstegi;
import com.otel.backend.exception.ApiHatasi;
import com.otel.backend.repository.GorunumRepository;
import com.otel.backend.repository.KonaklamaRepository;
import com.otel.backend.repository.ProsedurRepository;
import com.otel.backend.security.OturumKullanicisi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.otel.backend.dto.MisafirIstegi;
import com.otel.backend.entity.Misafir;
import com.otel.backend.repository.MisafirRepository;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ResepsiyonService {

    private final GorunumRepository gorunumRepo;
    private final ProsedurRepository prosedurRepo;
    private final KonaklamaRepository konaklamaRepo;
    private final MisafirRepository misafirRepo;

    // ------------------------------------------------------------ listeler (view'lar)

    public List<Map<String, Object>> bugun()          { return gorunumRepo.bugunGirisCikis(); }
    public List<Map<String, Object>> odaDurumu()      { return gorunumRepo.odaGuncelDurum(); }
    public List<Map<String, Object>> aktifKonaklamalar() { return gorunumRepo.aktifKonaklamalar(); }

    // ------------------------------------------------------------ check-in / check-out

    /** sp_CheckIn: konaklama kaydı açılır, trigger odayı DOLU yapar. */
    public Map<String, Object> checkIn(OturumKullanicisi ben, CheckInIstegi istek) {
        return prosedurRepo.checkIn(istek.rezervasyonId(), personelId(ben));
    }

    /**
     * sp_CheckOut — TEK TRANSACTION:
     * konaklama kapatılır, ödeme kaydedilir, oda TEMIZLIKTE olur.
     * Herhangi bir adım başarısız olursa veritabanı hepsini geri alır (ROLLBACK).
     */
    public Map<String, Object> checkOut(OturumKullanicisi ben, CheckOutIstegi istek) {
        return prosedurRepo.checkOut(istek.konaklamaId(), personelId(ben),
                istek.odemeTuru(), istek.tutar());
    }

    // ------------------------------------------------------------ hesap dökümü

    /** sp_KonaklamaToplamTutar (gece dökümü + hizmetler + özet) ve alınan ödemeler. */
    public Map<String, Object> konaklamaDetay(int konaklamaId) {
        if (!konaklamaRepo.existsById(konaklamaId)) {
            throw ApiHatasi.bulunamadi("Konaklama bulunamadı.");
        }
        Map<String, Object> detay = new LinkedHashMap<>(prosedurRepo.konaklamaToplamTutar(konaklamaId));
        detay.put("odemeler", gorunumRepo.konaklamaOdemeleri(konaklamaId));
        return detay;
    }
        // ------------------------------------------------------------ misafirler

    public List<Map<String, Object>> misafirAra(String q) {
        return gorunumRepo.misafirAra(q);
    }

    /** Aynı e-posta veya kimlik no varsa veritabanındaki UNIQUE kısıtı 409 döndürür. */
    public Map<String, Object> misafirEkle(MisafirIstegi istek) {
        Misafir m = new Misafir();
        m.setAd(istek.ad().trim());
        m.setSoyad(istek.soyad().trim());
        m.setKimlikNo(istek.kimlikNo().trim());
        m.setEposta(istek.eposta().trim().toLowerCase(Locale.ROOT));
        m.setTelefon(istek.telefon());
        m.setDogumTarihi(istek.dogumTarihi());
        misafirRepo.save(m);

        Map<String, Object> cevap = new LinkedHashMap<>();
        cevap.put("misafirId", m.getId());
        cevap.put("adSoyad", m.getAd() + " " + m.getSoyad());
        cevap.put("eposta", m.getEposta());
        return cevap;
    }

    // ------------------------------------------------------------ yardımcı

    private int personelId(OturumKullanicisi ben) {
        if (ben.personelId() == null) {
            throw ApiHatasi.yasak("Bu işlem yalnızca personel tarafından yapılabilir.");
        }
        return ben.personelId();
    }
}