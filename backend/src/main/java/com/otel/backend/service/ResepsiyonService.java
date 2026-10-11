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

@Service
@RequiredArgsConstructor
public class ResepsiyonService {

    private final GorunumRepository gorunumRepo;
    private final ProsedurRepository prosedurRepo;
    private final KonaklamaRepository konaklamaRepo;

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

    // ------------------------------------------------------------ yardımcı

    private int personelId(OturumKullanicisi ben) {
        if (ben.personelId() == null) {
            throw ApiHatasi.yasak("Bu işlem yalnızca personel tarafından yapılabilir.");
        }
        return ben.personelId();
    }
}