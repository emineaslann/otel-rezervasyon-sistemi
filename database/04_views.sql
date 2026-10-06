-- =====================================================================
--  04 - GÖRÜNÜMLER (VIEW)
-- =====================================================================
SET NAMES utf8mb4;
USE otel_db;

-- ---------------------------------------------------------------------
-- (1) Bugün giriş ve çıkış yapacak misafirler
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_BugunGirisCikis AS
SELECT 'GIRIS'                    AS Hareket,
       r.RezervasyonID,
       k.KonaklamaID,
       m.MisafirID,
       CONCAT(m.Ad, ' ', m.Soyad) AS MisafirAdSoyad,
       m.Telefon,
       o.OdaID, o.OdaNo, ot.TipAdi,
       o.Durum                    AS OdaDurumu,
       r.GirisTarihi, r.CikisTarihi, r.KisiSayisi,
       r.Durum                    AS RezervasyonDurumu,
       CASE WHEN k.KonaklamaID IS NULL THEN 'BEKLIYOR' ELSE 'YAPILDI' END AS IslemDurumu
  FROM Rezervasyon r
  JOIN Misafir m  ON m.MisafirID  = r.MisafirID
  JOIN Oda o      ON o.OdaID      = r.OdaID
  JOIN OdaTipi ot ON ot.OdaTipiID = o.OdaTipiID
  LEFT JOIN Konaklama k ON k.RezervasyonID = r.RezervasyonID
 WHERE r.GirisTarihi = CURDATE()
   AND r.Durum IN ('BEKLEMEDE','ONAYLI','TAMAMLANDI')
UNION ALL
SELECT 'CIKIS',
       r.RezervasyonID,
       k.KonaklamaID,
       m.MisafirID,
       CONCAT(m.Ad, ' ', m.Soyad),
       m.Telefon,
       o.OdaID, o.OdaNo, ot.TipAdi,
       o.Durum,
       r.GirisTarihi, r.CikisTarihi, r.KisiSayisi,
       r.Durum,
       CASE WHEN k.GercekCikis IS NULL THEN 'BEKLIYOR' ELSE 'YAPILDI' END
  FROM Rezervasyon r
  JOIN Konaklama k ON k.RezervasyonID = r.RezervasyonID
  JOIN Misafir m   ON m.MisafirID  = r.MisafirID
  JOIN Oda o       ON o.OdaID      = r.OdaID
  JOIN OdaTipi ot  ON ot.OdaTipiID = o.OdaTipiID
 WHERE r.CikisTarihi = CURDATE()
    OR (k.GercekCikis IS NULL AND r.CikisTarihi < CURDATE());   -- gecikmiş çıkışlar


    
-- ---------------------------------------------------------------------
-- (2) Oda bazında güncel durum ve son temizlik tarihi
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_OdaGuncelDurum AS
SELECT o.OdaID, o.OdaNo, o.Kat,
       ot.OdaTipiID, ot.TipAdi, ot.Kapasite,
       o.Durum,
       tk.SonTemizlik,
       tk.SonTemizlikPersonel,
       ak.KonaklamaID    AS AktifKonaklamaID,
       ak.MisafirAdSoyad AS AktifMisafir,
       ak.CikisTarihi    AS PlanlananCikis,
       (SELECT MIN(r2.GirisTarihi) FROM Rezervasyon r2
         WHERE r2.OdaID = o.OdaID
           AND r2.Durum IN ('BEKLEMEDE','ONAYLI')
           AND r2.GirisTarihi >= CURDATE()
           AND NOT EXISTS (SELECT 1 FROM Konaklama k2 WHERE k2.RezervasyonID = r2.RezervasyonID)
       ) AS SiradakiGiris
  FROM Oda o
  JOIN OdaTipi ot ON ot.OdaTipiID = o.OdaTipiID
  -- her odanın EN SON tamamlanan temizlik kaydı
  LEFT JOIN (
        SELECT t.OdaID,
               t.Tarih AS SonTemizlik,
               CONCAT(p.Ad, ' ', p.Soyad) AS SonTemizlikPersonel
          FROM TemizlikKaydi t
          JOIN Personel p ON p.PersonelID = t.PersonelID
         WHERE t.Durum = 'TAMAMLANDI'
           AND t.TemizlikKaydiID = (SELECT t2.TemizlikKaydiID FROM TemizlikKaydi t2
                                     WHERE t2.OdaID = t.OdaID AND t2.Durum = 'TAMAMLANDI'
                                     ORDER BY t2.Tarih DESC, t2.TemizlikKaydiID DESC
                                     LIMIT 1)
  ) tk ON tk.OdaID = o.OdaID
  -- odada şu an kalan misafir (çıkış yapılmamış konaklama)
  LEFT JOIN (
        SELECT r.OdaID, k.KonaklamaID, r.CikisTarihi,
               CONCAT(m.Ad, ' ', m.Soyad) AS MisafirAdSoyad
          FROM Konaklama k
          JOIN Rezervasyon r ON r.RezervasyonID = k.RezervasyonID
          JOIN Misafir m     ON m.MisafirID = r.MisafirID
         WHERE k.GercekCikis IS NULL
  ) ak ON ak.OdaID = o.OdaID;


  
-- ---------------------------------------------------------------------
-- Ek: şu an otelde kalanlar ve anlık hesapları
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_AktifKonaklamalar AS
SELECT k.KonaklamaID, k.RezervasyonID, k.GercekGiris,
       r.GirisTarihi, r.CikisTarihi, r.KisiSayisi,
       m.MisafirID, CONCAT(m.Ad, ' ', m.Soyad) AS MisafirAdSoyad, m.Telefon,
       o.OdaID, o.OdaNo, ot.TipAdi,
       fn_KonaklamaToplam(k.KonaklamaID) AS GuncelToplam,
       (SELECT COALESCE(SUM(od.Tutar), 0) FROM Odeme od
         WHERE od.KonaklamaID = k.KonaklamaID) AS Odenen
  FROM Konaklama k
  JOIN Rezervasyon r ON r.RezervasyonID = k.RezervasyonID
  JOIN Misafir m     ON m.MisafirID = r.MisafirID
  JOIN Oda o         ON o.OdaID = r.OdaID
  JOIN OdaTipi ot    ON ot.OdaTipiID = o.OdaTipiID
 WHERE k.GercekCikis IS NULL;

-- ---------------------------------------------------------------------
-- Ek: misafirin tüm rezervasyon ve konaklama geçmişi
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_MisafirRezervasyonlari AS
SELECT r.RezervasyonID, r.MisafirID, r.GirisTarihi, r.CikisTarihi, r.KisiSayisi,
       r.Durum, r.OlusturmaTarihi,
       DATEDIFF(r.CikisTarihi, r.GirisTarihi) AS GeceSayisi,
       o.OdaNo, ot.TipAdi,
       k.KonaklamaID, k.GercekGiris, k.GercekCikis,
       CASE WHEN k.KonaklamaID IS NOT NULL THEN fn_KonaklamaToplam(k.KonaklamaID)
            ELSE fn_KonaklamaUcreti(o.OdaTipiID, r.GirisTarihi, r.CikisTarihi)
       END AS Tutar,
       (SELECT COALESCE(SUM(od.Tutar), 0) FROM Odeme od
         WHERE od.KonaklamaID = k.KonaklamaID) AS Odenen
  FROM Rezervasyon r
  JOIN Oda o      ON o.OdaID = r.OdaID
  JOIN OdaTipi ot ON ot.OdaTipiID = o.OdaTipiID
  LEFT JOIN Konaklama k ON k.RezervasyonID = r.RezervasyonID;