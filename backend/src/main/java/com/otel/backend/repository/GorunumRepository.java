package com.otel.backend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** Veritabanındaki görünümlerin (view) ve IslemLog'un okunması. */
@Repository
@RequiredArgsConstructor
public class GorunumRepository {

    private final JdbcYardimci db;

    // (1) Hocanın istediği view: bugün giriş ve çıkış yapacaklar
    public List<Map<String, Object>> bugunGirisCikis() {
        return db.sorgu("SELECT * FROM vw_BugunGirisCikis ORDER BY Hareket DESC, IslemDurumu, OdaNo");
    }

    // (2) Hocanın istediği view: oda bazında güncel durum ve son temizlik
    public List<Map<String, Object>> odaGuncelDurum() {
        return db.sorgu("SELECT * FROM vw_OdaGuncelDurum ORDER BY Kat, OdaNo");
    }

    public List<Map<String, Object>> aktifKonaklamalar() {
        return db.sorgu("SELECT *, GuncelToplam - Odenen AS Bakiye FROM vw_AktifKonaklamalar ORDER BY OdaNo");
    }

    public List<Map<String, Object>> misafirRezervasyonlari(int misafirId) {
        return db.sorgu("SELECT * FROM vw_MisafirRezervasyonlari WHERE MisafirID = ? ORDER BY GirisTarihi DESC",
                misafirId);
    }

    /** Tetikleyicilerin doldurduğu işlem günlüğü (en yeniler önce). */
    public List<Map<String, Object>> islemLog(int limit) {
        return db.sorgu("SELECT * FROM IslemLog ORDER BY LogID DESC LIMIT ?", limit);
    }
}