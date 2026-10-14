package com.otel.backend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import java.time.LocalDate;
import java.util.ArrayList;

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
    
    /** Misafirin tamamlanmış (check-out yapılmış) konaklamaları. */
    public List<Map<String, Object>> misafirGecmisi(int misafirId) {
        return db.sorgu("""
                SELECT * FROM vw_MisafirRezervasyonlari
                 WHERE MisafirID = ? AND GercekCikis IS NOT NULL
                 ORDER BY GercekCikis DESC""", misafirId);
    }
    
    /** Bir konaklamada alınan ödemeler (hesap dökümü için). */
    public List<Map<String, Object>> konaklamaOdemeleri(int konaklamaId) {
        return db.sorgu("""
                SELECT od.OdemeID, od.Tutar, od.OdemeTuru, od.OdemeTarihi,
                       CONCAT(p.Ad, ' ', p.Soyad) AS PersonelAdSoyad
                  FROM Odeme od
                  LEFT JOIN Personel p ON p.PersonelID = od.PersonelID
                 WHERE od.KonaklamaID = ?
                 ORDER BY od.OdemeTarihi""", konaklamaId);
    }
    
    /** Resepsiyon misafir araması: ad, soyad, e-posta, telefon veya kimlik no. */
    public List<Map<String, Object>> misafirAra(String q) {
        String sql = """
                SELECT m.MisafirID, m.Ad, m.Soyad, m.KimlikNo, m.Telefon, m.Eposta, m.DogumTarihi,
                       (SELECT COUNT(*) FROM Rezervasyon r
                         WHERE r.MisafirID = m.MisafirID AND r.Durum = 'TAMAMLANDI') AS KonaklamaSayisi
                  FROM Misafir m""";
        if (q == null || q.isBlank()) {
            return db.sorgu(sql + " ORDER BY m.Soyad, m.Ad LIMIT 50");
        }
        String desen = "%" + q.trim() + "%";
                return db.sorgu(sql + "\n" + """
                 WHERE CONCAT(m.Ad, ' ', m.Soyad) LIKE ? OR m.Eposta LIKE ?
                    OR m.Telefon LIKE ? OR m.KimlikNo LIKE ?
                 ORDER BY m.Soyad, m.Ad LIMIT 50""", desen, desen, desen, desen);
    }

    /**
     * Çok tablolu JOIN ile rezervasyon listesi (Rezervasyon + Misafir + Oda + OdaTipi + Konaklama).
     * Filtreler isteğe bağlıdır; gelmeyen filtre sorguya eklenmez.
     */
    public List<Map<String, Object>> rezervasyonAra(String durum, LocalDate baslangic, LocalDate bitis,
                                                    String q, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.RezervasyonID, r.GirisTarihi, r.CikisTarihi, r.KisiSayisi, r.Durum, r.OlusturmaTarihi,
                       DATEDIFF(r.CikisTarihi, r.GirisTarihi) AS GeceSayisi,
                       m.MisafirID, CONCAT(m.Ad, ' ', m.Soyad) AS MisafirAdSoyad, m.Telefon,
                       o.OdaID, o.OdaNo, ot.TipAdi,
                       fn_KonaklamaUcreti(o.OdaTipiID, r.GirisTarihi, r.CikisTarihi) AS ToplamFiyat,
                       k.KonaklamaID
                  FROM Rezervasyon r
                  JOIN Misafir m  ON m.MisafirID  = r.MisafirID
                  JOIN Oda o      ON o.OdaID      = r.OdaID
                  JOIN OdaTipi ot ON ot.OdaTipiID = o.OdaTipiID
                  LEFT JOIN Konaklama k ON k.RezervasyonID = r.RezervasyonID
                 WHERE 1 = 1""");
        List<Object> p = new ArrayList<>();

        if (durum != null)    { sql.append(" AND r.Durum = ?");       p.add(durum); }
        if (baslangic != null){ sql.append(" AND r.CikisTarihi > ?"); p.add(baslangic); }
        if (bitis != null)    { sql.append(" AND r.GirisTarihi <= ?");p.add(bitis); }
        if (q != null && !q.isBlank()) {
            sql.append(" AND (CONCAT(m.Ad, ' ', m.Soyad) LIKE ? OR o.OdaNo = ? OR CAST(r.RezervasyonID AS CHAR) = ?)");
            p.add("%" + q.trim() + "%");
            p.add(q.trim());
            p.add(q.trim());
        }
        sql.append(" ORDER BY r.GirisTarihi DESC, r.RezervasyonID DESC LIMIT ?");
        p.add(limit);

        return db.sorgu(sql.toString(), p.toArray());
    }
    
    /** Oda durumlarının dağılımı: GROUP BY ile gruplama. */
    public List<Map<String, Object>> odaDurumDagilimi() {
        return db.sorgu("SELECT Durum, COUNT(*) AS Adet FROM Oda GROUP BY Durum ORDER BY Durum");
    }

    /** Yönetici panosu için anlık sayılar. */
    public Map<String, Object> genelOzet() {
        return db.sorgu("""
                SELECT (SELECT COUNT(*) FROM Oda) AS ToplamOda,
                       (SELECT COUNT(*) FROM Konaklama WHERE GercekCikis IS NULL) AS OteldekiMisafir,
                       (SELECT COUNT(*) FROM Rezervasyon WHERE Durum = 'BEKLEMEDE') AS OnayBekleyen,
                       (SELECT COUNT(*) FROM Rezervasyon WHERE GirisTarihi = CURDATE()
                          AND Durum IN ('BEKLEMEDE','ONAYLI')) AS BugunGiris,
                       (SELECT COALESCE(SUM(Tutar), 0) FROM Odeme
                         WHERE DATE(OdemeTarihi) = CURDATE()) AS BugunTahsilat""").get(0);
    }

    /** Son 90 günde en çok gelir getiren ek hizmetler: GROUP BY + HAVING. */
    public List<Map<String, Object>> populerHizmetler() {
        return db.sorgu("""
                SELECT eh.HizmetAdi, SUM(kh.Adet) AS Adet, SUM(kh.Adet * kh.BirimFiyat) AS Gelir
                  FROM KonaklamaHizmeti kh
                  JOIN EkHizmet eh ON eh.EkHizmetID = kh.EkHizmetID
                 WHERE kh.Tarih >= CURDATE() - INTERVAL 90 DAY
                 GROUP BY eh.EkHizmetID, eh.HizmetAdi
                HAVING SUM(kh.Adet) > 0
                 ORDER BY Gelir DESC""");
    }

    /** Tetikleyicilerin doldurduğu işlem günlüğü (en yeniler önce). */
    public List<Map<String, Object>> islemLog(int limit) {
        return db.sorgu("SELECT * FROM IslemLog ORDER BY LogID DESC LIMIT ?", limit);
    }
}