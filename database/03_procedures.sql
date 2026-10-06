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


-- =====================================================================
-- sp_CheckOut — ZORUNLU TRANSACTION
--   Adımlar (hepsi ya birlikte olur ya hiç olmaz):
--     1) Konaklama kapatılır (GercekCikis)  -> trigger odayı TEMIZLIKTE yapar
--     2) Kalan tutar için ödeme kaydedilir
--     3) Ödeme yeterliliği doğrulanır, rezervasyon TAMAMLANDI yapılır
--     4) Oda durumunun güncellendiği doğrulanır
--   Herhangi bir adım hata verirse EXIT HANDLER -> ROLLBACK
--   p_tutar NULL ise kalan tutarın tamamı tahsil edilir.
-- =====================================================================
DROP PROCEDURE IF EXISTS sp_CheckOut $$
CREATE PROCEDURE sp_CheckOut(
    IN p_konaklamaID INT UNSIGNED,
    IN p_personelID  INT UNSIGNED,
    IN p_odemeTuru   VARCHAR(10),
    IN p_tutar       DECIMAL(12,2)
)
BEGIN
    DECLARE v_rezID    INT UNSIGNED;
    DECLARE v_odaID    INT UNSIGNED;
    DECLARE v_cikis    DATETIME;
    DECLARE v_toplam   DECIMAL(12,2);
    DECLARE v_odenen   DECIMAL(12,2);
    DECLARE v_kalan    DECIMAL(12,2);
    DECLARE v_alinan   DECIMAL(12,2);
    DECLARE v_odaDurum VARCHAR(20);
    DECLARE v_mesaj    VARCHAR(255);

    -- Herhangi bir SQL hatasında: önce geri al, sonra hatayı dışarı ilet
    DECLARE EXIT HANDLER FOR SQLEXCEPTION, SQLWARNING
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
        SELECT k.RezervasyonID, k.GercekCikis, r.OdaID
          INTO v_rezID, v_cikis, v_odaID
          FROM Konaklama k
          JOIN Rezervasyon r ON r.RezervasyonID = k.RezervasyonID
         WHERE k.KonaklamaID = p_konaklamaID
           FOR UPDATE;

        IF v_rezID IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Konaklama bulunamadı.';
        END IF;
        IF v_cikis IS NOT NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bu konaklama için check-out zaten yapılmış.';
        END IF;

        SET v_toplam = fn_KonaklamaToplam(p_konaklamaID);
        SELECT COALESCE(SUM(Tutar), 0) INTO v_odenen FROM Odeme WHERE KonaklamaID = p_konaklamaID;
        SET v_kalan  = v_toplam - v_odenen;
        SET v_alinan = COALESCE(p_tutar, v_kalan);

        -- Adım 1: konaklamayı kapat (trigger -> oda TEMIZLIKTE)
        UPDATE Konaklama
           SET GercekCikis = NOW(), CikisPersonelID = p_personelID
         WHERE KonaklamaID = p_konaklamaID;

        -- Adım 2: ödemeyi kaydet (Tutar > 0 ve ödeme türü kısıtları burada devreye girer)
        IF v_kalan > 0 OR p_tutar IS NOT NULL THEN
            INSERT INTO Odeme (KonaklamaID, Tutar, OdemeTuru, OdemeTarihi, PersonelID)
            VALUES (p_konaklamaID, v_alinan, p_odemeTuru, NOW(), p_personelID);
        END IF;

        -- Adım 3: hesap kapanmış olmalı
        IF v_odenen + IF(v_kalan > 0 OR p_tutar IS NOT NULL, v_alinan, 0) < v_toplam THEN
            SET v_mesaj = CONCAT('Ödeme yetersiz: toplam ', v_toplam, ' TL, ödenen ',
                                 v_odenen + v_alinan, ' TL. İşlem geri alındı.');
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_mesaj;
        END IF;

        UPDATE Rezervasyon SET Durum = 'TAMAMLANDI' WHERE RezervasyonID = v_rezID;

        -- Adım 4: oda durumunun güncellendiğini doğrula
        SELECT Durum INTO v_odaDurum FROM Oda WHERE OdaID = v_odaID;
        IF v_odaDurum <> 'TEMIZLIKTE' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Oda durumu güncellenemedi. İşlem geri alındı.';
        END IF;
    COMMIT;

    SELECT p_konaklamaID AS KonaklamaID, v_toplam AS GenelToplam,
           v_odenen AS OncedenOdenen, v_alinan AS AlinanOdeme,
           v_odaDurum AS OdaDurumu, 'TAMAMLANDI' AS RezervasyonDurumu;
END $$



-- =====================================================================
-- sp_RezervasyonOlustur
--   Oda satırı FOR UPDATE ile kilitlenir: aynı oda için eşzamanlı iki istek
--   sıraya girer, tetikleyicinin çakışma kontrolü yarış durumunda da güvenli olur.
-- =====================================================================
DROP PROCEDURE IF EXISTS sp_RezervasyonOlustur $$
CREATE PROCEDURE sp_RezervasyonOlustur(
    IN p_misafirID  INT UNSIGNED,
    IN p_odaID      INT UNSIGNED,
    IN p_giris      DATE,
    IN p_cikis      DATE,
    IN p_kisi       TINYINT UNSIGNED,
    IN p_durum      VARCHAR(20)
)
BEGIN
    DECLARE v_durum    VARCHAR(20);
    DECLARE v_odaDurum VARCHAR(20);
    DECLARE v_id       INT UNSIGNED;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION, SQLWARNING
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_durum = COALESCE(p_durum, 'BEKLEMEDE');
    IF v_durum NOT IN ('BEKLEMEDE','ONAYLI') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Yeni rezervasyon BEKLEMEDE veya ONAYLI olabilir.';
    END IF;
    IF p_giris < CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Geçmiş tarihli rezervasyon oluşturulamaz.';
    END IF;

    START TRANSACTION;
        SELECT Durum INTO v_odaDurum FROM Oda WHERE OdaID = p_odaID FOR UPDATE;
        IF v_odaDurum IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Oda bulunamadı.';
        END IF;
        IF v_odaDurum = 'BAKIMDA' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Oda bakımda olduğu için rezerve edilemez.';
        END IF;

        -- çakışma ve kapasite kontrolü trigger'da (trg_Rezervasyon_Cakisma_BI)
        INSERT INTO Rezervasyon (MisafirID, OdaID, GirisTarihi, CikisTarihi, KisiSayisi, Durum)
        VALUES (p_misafirID, p_odaID, p_giris, p_cikis, p_kisi, v_durum);
        SET v_id = LAST_INSERT_ID();
    COMMIT;

    SELECT r.RezervasyonID, r.Durum, r.GirisTarihi, r.CikisTarihi, r.KisiSayisi,
           o.OdaNo, ot.TipAdi,
           DATEDIFF(r.CikisTarihi, r.GirisTarihi) AS GeceSayisi,
           fn_KonaklamaUcreti(o.OdaTipiID, r.GirisTarihi, r.CikisTarihi) AS ToplamFiyat
      FROM Rezervasyon r
      JOIN Oda o      ON o.OdaID = r.OdaID
      JOIN OdaTipi ot ON ot.OdaTipiID = o.OdaTipiID
     WHERE r.RezervasyonID = v_id;
END $$

-- ---------------------------------------------------------------------
-- sp_RezervasyonIptal  (p_misafirID verilirse sahiplik kontrolü yapılır)
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_RezervasyonIptal $$
CREATE PROCEDURE sp_RezervasyonIptal(IN p_rezID INT UNSIGNED, IN p_misafirID INT UNSIGNED)
BEGIN
    DECLARE v_durum   VARCHAR(20);
    DECLARE v_misafir INT UNSIGNED;

    SELECT Durum, MisafirID INTO v_durum, v_misafir
      FROM Rezervasyon WHERE RezervasyonID = p_rezID;

    IF v_durum IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rezervasyon bulunamadı.';
    END IF;
    IF p_misafirID IS NOT NULL AND v_misafir <> p_misafirID THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bu rezervasyon size ait değil.';
    END IF;
    IF v_durum NOT IN ('BEKLEMEDE','ONAYLI') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Yalnızca bekleyen veya onaylı rezervasyonlar iptal edilebilir.';
    END IF;
    IF EXISTS (SELECT 1 FROM Konaklama WHERE RezervasyonID = p_rezID) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Check-in yapılmış rezervasyon iptal edilemez.';
    END IF;

    UPDATE Rezervasyon SET Durum = 'IPTAL' WHERE RezervasyonID = p_rezID;
    SELECT p_rezID AS RezervasyonID, 'IPTAL' AS Durum;
END $$

-- ---------------------------------------------------------------------
-- sp_RezervasyonOnayla
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_RezervasyonOnayla $$
CREATE PROCEDURE sp_RezervasyonOnayla(IN p_rezID INT UNSIGNED)
BEGIN
    DECLARE v_durum VARCHAR(20);

    SELECT Durum INTO v_durum FROM Rezervasyon WHERE RezervasyonID = p_rezID;
    IF v_durum IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rezervasyon bulunamadı.';
    END IF;
    IF v_durum <> 'BEKLEMEDE' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Yalnızca beklemedeki rezervasyon onaylanabilir.';
    END IF;

    UPDATE Rezervasyon SET Durum = 'ONAYLI' WHERE RezervasyonID = p_rezID;

    -- e-posta bildirimi için gereken bilgiler
    SELECT r.RezervasyonID, r.Durum, r.GirisTarihi, r.CikisTarihi, r.KisiSayisi,
           m.Ad, m.Soyad, m.Eposta, o.OdaNo, ot.TipAdi,
           fn_KonaklamaUcreti(o.OdaTipiID, r.GirisTarihi, r.CikisTarihi) AS ToplamFiyat
      FROM Rezervasyon r
      JOIN Misafir m  ON m.MisafirID = r.MisafirID
      JOIN Oda o      ON o.OdaID = r.OdaID
      JOIN OdaTipi ot ON ot.OdaTipiID = o.OdaTipiID
     WHERE r.RezervasyonID = p_rezID;
END $$


-- ---------------------------------------------------------------------
-- sp_CheckIn (transaction) -> Konaklama INSERT, trigger odayı DOLU yapar
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_CheckIn $$
CREATE PROCEDURE sp_CheckIn(IN p_rezID INT UNSIGNED, IN p_personelID INT UNSIGNED)
BEGIN
    DECLARE v_durum    VARCHAR(20);
    DECLARE v_giris    DATE;
    DECLARE v_cikis    DATE;
    DECLARE v_odaID    INT UNSIGNED;
    DECLARE v_odaDurum VARCHAR(20);
    DECLARE v_kid      INT UNSIGNED;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION, SQLWARNING
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
        SELECT Durum, GirisTarihi, CikisTarihi, OdaID
          INTO v_durum, v_giris, v_cikis, v_odaID
          FROM Rezervasyon WHERE RezervasyonID = p_rezID FOR UPDATE;

        IF v_durum IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rezervasyon bulunamadı.';
        END IF;
        IF v_durum NOT IN ('BEKLEMEDE','ONAYLI') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bu rezervasyon için check-in yapılamaz (iptal veya tamamlanmış).';
        END IF;
        IF EXISTS (SELECT 1 FROM Konaklama WHERE RezervasyonID = p_rezID) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bu rezervasyon için zaten check-in yapılmış.';
        END IF;
        IF CURDATE() < v_giris OR CURDATE() >= v_cikis THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Check-in yalnızca rezervasyon tarihleri içinde yapılabilir.';
        END IF;

        SELECT Durum INTO v_odaDurum FROM Oda WHERE OdaID = v_odaID FOR UPDATE;
        IF v_odaDurum <> 'BOS' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Oda şu an hazır değil (dolu, temizlikte veya bakımda).';
        END IF;

        -- Onaylanmamış rezervasyonla gelen misafir, check-in anında onaylanmış sayılır
        IF v_durum = 'BEKLEMEDE' THEN
            UPDATE Rezervasyon SET Durum = 'ONAYLI' WHERE RezervasyonID = p_rezID;
        END IF;

        INSERT INTO Konaklama (RezervasyonID, GercekGiris, GirisPersonelID)
        VALUES (p_rezID, NOW(), p_personelID);       -- trigger -> oda DOLU
        SET v_kid = LAST_INSERT_ID();
    COMMIT;

    SELECT k.KonaklamaID, k.RezervasyonID, k.GercekGiris, o.OdaNo, o.Durum AS OdaDurumu
      FROM Konaklama k
      JOIN Rezervasyon r ON r.RezervasyonID = k.RezervasyonID
      JOIN Oda o ON o.OdaID = r.OdaID
     WHERE k.KonaklamaID = v_kid;
END $$

DELIMITER ;