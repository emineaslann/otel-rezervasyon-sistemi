package com.otel.backend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Veritabanındaki saklı prosedürlerin Java karşılıkları. */
@Repository
@RequiredArgsConstructor
public class ProsedurRepository {

    private final JdbcYardimci db;

    // (1) Hocanın istediği prosedür: boş odalar
    public List<Map<String, Object>> bosOdalar(LocalDate giris, LocalDate cikis,
                                               Integer odaTipiId, Integer kisiSayisi) {
        return db.prosedurCagir("sp_BosOdalariListele", giris, cikis, odaTipiId, kisiSayisi).get(0);
    }

    // (2) Hocanın istediği prosedür: konaklama toplam tutarı (3 tablo döner)
    public Map<String, Object> konaklamaToplamTutar(int konaklamaId) {
        List<List<Map<String, Object>>> s = db.prosedurCagir("sp_KonaklamaToplamTutar", konaklamaId);
        Map<String, Object> sonuc = new LinkedHashMap<>();
        sonuc.put("geceler", s.get(0));
        sonuc.put("hizmetler", s.get(1));
        sonuc.put("ozet", s.get(2).isEmpty() ? Map.of() : s.get(2).get(0));
        return sonuc;
    }

    // (3) Hocanın istediği prosedür: aylık doluluk ve gelir raporu (3 tablo döner)
    public Map<String, Object> aylikRapor(int yil, int ay) {
        List<List<Map<String, Object>>> s = db.prosedurCagir("sp_AylikDolulukGelirRaporu", yil, ay);
        Map<String, Object> sonuc = new LinkedHashMap<>();
        sonuc.put("ozet", s.get(0).get(0));
        sonuc.put("odaTipleri", s.get(1));
        sonuc.put("gunluk", s.get(2));
        return sonuc;
    }

    public List<Map<String, Object>> yillikOzet(int yil) {
        return db.prosedurCagir("sp_YillikGelirOzeti", yil).get(0);
    }

    // Rezervasyon işlemleri
    public Map<String, Object> rezervasyonOlustur(int misafirId, int odaId, LocalDate giris,
                                                  LocalDate cikis, int kisi, String durum) {
        return db.prosedurTekSatir("sp_RezervasyonOlustur", misafirId, odaId, giris, cikis, kisi, durum);
    }

    public Map<String, Object> rezervasyonIptal(int rezervasyonId, Integer misafirId) {
        return db.prosedurTekSatir("sp_RezervasyonIptal", rezervasyonId, misafirId);
    }

    public Map<String, Object> rezervasyonOnayla(int rezervasyonId) {
        return db.prosedurTekSatir("sp_RezervasyonOnayla", rezervasyonId);
    }

    // Check-in / check-out (ikisi de veritabanında TRANSACTION)
    public Map<String, Object> checkIn(int rezervasyonId, int personelId) {
        return db.prosedurTekSatir("sp_CheckIn", rezervasyonId, personelId);
    }

    public Map<String, Object> checkOut(int konaklamaId, int personelId, String odemeTuru, BigDecimal tutar) {
        return db.prosedurTekSatir("sp_CheckOut", konaklamaId, personelId, odemeTuru, tutar);
    }
}