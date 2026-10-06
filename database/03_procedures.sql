-- =====================================================================
--  03 - FONKSİYONLAR ve SAKLI PROSEDÜRLER
-- =====================================================================
SET NAMES utf8mb4;
USE otel_db;

DELIMITER $$

-- ---------------------------------------------------------------------
-- fn_GecelikFiyat: bir oda tipinin belirli bir gecedeki fiyatı
--   Sezon tanımı varsa sezon fiyatı, yoksa OdaTipi.TemelFiyat
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_GecelikFiyat $$
CREATE FUNCTION fn_GecelikFiyat(p_odaTipiID INT UNSIGNED, p_gece DATE)
RETURNS DECIMAL(10,2)
READS SQL DATA
BEGIN
    DECLARE v_fiyat DECIMAL(10,2) DEFAULT NULL;

    SELECT s.GecelikFiyat INTO v_fiyat
      FROM SezonFiyati s
     WHERE s.OdaTipiID = p_odaTipiID
       AND p_gece BETWEEN s.BaslangicTarihi AND s.BitisTarihi
     LIMIT 1;

    IF v_fiyat IS NULL THEN
        SELECT TemelFiyat INTO v_fiyat FROM OdaTipi WHERE OdaTipiID = p_odaTipiID;
    END IF;

    RETURN v_fiyat;
END $$


-- ---------------------------------------------------------------------
-- fn_KonaklamaUcreti: [giris, cikis) aralığındaki her gecenin fiyatı toplamı
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_KonaklamaUcreti $$
CREATE FUNCTION fn_KonaklamaUcreti(p_odaTipiID INT UNSIGNED, p_giris DATE, p_cikis DATE)
RETURNS DECIMAL(12,2)
READS SQL DATA
BEGIN
    DECLARE v_toplam DECIMAL(12,2) DEFAULT 0;
    DECLARE v_gece DATE;

    SET v_gece = p_giris;
    WHILE v_gece < p_cikis DO
        SET v_toplam = v_toplam + fn_GecelikFiyat(p_odaTipiID, v_gece);
        SET v_gece = DATE_ADD(v_gece, INTERVAL 1 DAY);
    END WHILE;

    RETURN v_toplam;
END $$



-- =====================================================================
-- (1) sp_BosOdalariListele
--     p_odaTipiID NULL ise tüm tipler; p_kisiSayisi NULL ise kapasite filtresi yok
-- =====================================================================
DROP PROCEDURE IF EXISTS sp_BosOdalariListele $$
CREATE PROCEDURE sp_BosOdalariListele(
    IN p_giris      DATE,
    IN p_cikis      DATE,
    IN p_odaTipiID  INT UNSIGNED,
    IN p_kisiSayisi TINYINT UNSIGNED
)
BEGIN
    IF p_giris IS NULL OR p_cikis IS NULL OR p_cikis <= p_giris THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Çıkış tarihi giriş tarihinden sonra olmalı.';
    END IF;

    SELECT o.OdaID, o.OdaNo, o.Kat, ot.OdaTipiID, ot.TipAdi, ot.Kapasite, ot.Aciklama,
           DATEDIFF(p_cikis, p_giris)                         AS GeceSayisi,
           fn_KonaklamaUcreti(ot.OdaTipiID, p_giris, p_cikis) AS ToplamFiyat,
           ROUND(fn_KonaklamaUcreti(ot.OdaTipiID, p_giris, p_cikis)
                 / DATEDIFF(p_cikis, p_giris), 2)             AS OrtalamaGecelik
      FROM Oda o
      JOIN OdaTipi ot ON ot.OdaTipiID = o.OdaTipiID
     WHERE o.Durum <> 'BAKIMDA'
       AND (p_odaTipiID IS NULL OR o.OdaTipiID = p_odaTipiID)
       AND (p_kisiSayisi IS NULL OR ot.Kapasite >= p_kisiSayisi)
       AND NOT EXISTS (
            SELECT 1 FROM Rezervasyon r
             WHERE r.OdaID = o.OdaID
               AND r.Durum IN ('BEKLEMEDE','ONAYLI')
               AND r.GirisTarihi < p_cikis
               AND p_giris < r.CikisTarihi)
     ORDER BY ot.TemelFiyat, o.OdaNo;
END $$


-- ---------------------------------------------------------------------
-- fn_KonaklamaToplam: oda ücreti (sezonluk) + ek hizmetler
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_KonaklamaToplam $$
CREATE FUNCTION fn_KonaklamaToplam(p_konaklamaID INT UNSIGNED)
RETURNS DECIMAL(12,2)
READS SQL DATA
BEGIN
    DECLARE v_oda DECIMAL(12,2) DEFAULT 0;
    DECLARE v_hizmet DECIMAL(12,2) DEFAULT 0;

    SELECT fn_KonaklamaUcreti(o.OdaTipiID, r.GirisTarihi, r.CikisTarihi) INTO v_oda
      FROM Konaklama k
      JOIN Rezervasyon r ON r.RezervasyonID = k.RezervasyonID
      JOIN Oda o         ON o.OdaID = r.OdaID
     WHERE k.KonaklamaID = p_konaklamaID;

    SELECT COALESCE(SUM(kh.Adet * kh.BirimFiyat), 0) INTO v_hizmet
      FROM KonaklamaHizmeti kh
     WHERE kh.KonaklamaID = p_konaklamaID;

    RETURN v_oda + v_hizmet;
END $$

-- =====================================================================
-- (2) sp_KonaklamaToplamTutar
--     Sonuç 1: gece gece fiyat dökümü
--     Sonuç 2: ek hizmet dökümü
--     Sonuç 3: özet (oda + hizmet = toplam, ödenen, kalan)
-- =====================================================================
DROP PROCEDURE IF EXISTS sp_KonaklamaToplamTutar $$
CREATE PROCEDURE sp_KonaklamaToplamTutar(IN p_konaklamaID INT UNSIGNED)
BEGIN
    DECLARE v_tip    INT UNSIGNED;
    DECLARE v_giris  DATE;
    DECLARE v_cikis  DATE;
    DECLARE v_oda    DECIMAL(12,2);
    DECLARE v_hizmet DECIMAL(12,2);
    DECLARE v_odenen DECIMAL(12,2);

    SELECT o.OdaTipiID, r.GirisTarihi, r.CikisTarihi
      INTO v_tip, v_giris, v_cikis
      FROM Konaklama k
      JOIN Rezervasyon r ON r.RezervasyonID = k.RezervasyonID
      JOIN Oda o         ON o.OdaID = r.OdaID
     WHERE k.KonaklamaID = p_konaklamaID;

    IF v_tip IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Konaklama bulunamadı.';
    END IF;

    -- 1) gece dökümü: özyinelemeli CTE her geceyi bir satır olarak üretir
    WITH RECURSIVE geceler (Gece) AS (
        SELECT v_giris
        UNION ALL
        SELECT DATE_ADD(Gece, INTERVAL 1 DAY) FROM geceler
         WHERE DATE_ADD(Gece, INTERVAL 1 DAY) < v_cikis
    )
    SELECT g.Gece,
           COALESCE((SELECT s.SezonAdi FROM SezonFiyati s
                      WHERE s.OdaTipiID = v_tip
                        AND g.Gece BETWEEN s.BaslangicTarihi AND s.BitisTarihi
                      LIMIT 1), 'Temel fiyat') AS Sezon,
           fn_GecelikFiyat(v_tip, g.Gece)        AS Fiyat
      FROM geceler g
     ORDER BY g.Gece;

    -- 2) ek hizmet dökümü
    SELECT kh.KonaklamaHizmetiID, eh.HizmetAdi, kh.Adet, kh.BirimFiyat,
           kh.Adet * kh.BirimFiyat AS Tutar, kh.Tarih
      FROM KonaklamaHizmeti kh
      JOIN EkHizmet eh ON eh.EkHizmetID = kh.EkHizmetID
     WHERE kh.KonaklamaID = p_konaklamaID
     ORDER BY kh.Tarih;

    -- 3) özet
    SET v_oda = fn_KonaklamaUcreti(v_tip, v_giris, v_cikis);
    SELECT COALESCE(SUM(Adet * BirimFiyat), 0) INTO v_hizmet
      FROM KonaklamaHizmeti WHERE KonaklamaID = p_konaklamaID;
    SELECT COALESCE(SUM(Tutar), 0) INTO v_odenen
      FROM Odeme WHERE KonaklamaID = p_konaklamaID;

    SELECT p_konaklamaID                           AS KonaklamaID,
           DATEDIFF(v_cikis, v_giris)              AS GeceSayisi,
           v_oda                                   AS OdaUcreti,
           v_hizmet                                AS HizmetToplami,
           v_oda + v_hizmet                        AS GenelToplam,
           v_odenen                                AS Odenen,
           GREATEST(v_oda + v_hizmet - v_odenen, 0) AS Kalan;
END $$
DELIMITER ;