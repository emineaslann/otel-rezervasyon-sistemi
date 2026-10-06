-- =====================================================================
--  02 - TETİKLEYİCİLER (TRIGGER)
-- =====================================================================
SET NAMES utf8mb4;
USE otel_db;

DELIMITER $$

-- ---------------------------------------------------------------------
-- T1-a  Yeni rezervasyonda çakışma + kapasite kontrolü
--   [a1,a2) ve [b1,b2) aralıkları çakışır  <=>  a1 < b2 AND b1 < a2
--   Yalnızca aktif rezervasyonlar (BEKLEMEDE, ONAYLI) odayı bloke eder.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_Rezervasyon_Cakisma_BI $$
CREATE TRIGGER trg_Rezervasyon_Cakisma_BI
BEFORE INSERT ON Rezervasyon
FOR EACH ROW
BEGIN
    DECLARE v_cakisan INT UNSIGNED DEFAULT NULL;
    DECLARE v_kapasite TINYINT UNSIGNED;
    DECLARE v_mesaj VARCHAR(255);

    -- Kapasite kontrolü: odanın tipinden kapasiteyi bul
    SELECT ot.Kapasite INTO v_kapasite
      FROM Oda o
      JOIN OdaTipi ot ON ot.OdaTipiID = o.OdaTipiID
     WHERE o.OdaID = NEW.OdaID;

    IF NEW.KisiSayisi > v_kapasite THEN
        SET v_mesaj = CONCAT('Kişi sayısı oda kapasitesini aşıyor (kapasite: ', v_kapasite, ').');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_mesaj;
    END IF;

    -- Çakışma kontrolü
    IF NEW.Durum IN ('BEKLEMEDE','ONAYLI') THEN
        SELECT r.RezervasyonID INTO v_cakisan
          FROM Rezervasyon r
         WHERE r.OdaID = NEW.OdaID
           AND r.Durum IN ('BEKLEMEDE','ONAYLI')
           AND r.GirisTarihi < NEW.CikisTarihi
           AND NEW.GirisTarihi < r.CikisTarihi
         LIMIT 1;

        IF v_cakisan IS NOT NULL THEN
            SET v_mesaj = CONCAT('Oda bu tarihlerde dolu: #', v_cakisan,
                                 ' numaralı rezervasyonla çakışıyor.');
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_mesaj;
        END IF;
    END IF;
END $$


-- ---------------------------------------------------------------------
-- T1-b  Rezervasyon güncellemesinde çakışma kontrolü
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_Rezervasyon_Cakisma_BU $$
CREATE TRIGGER trg_Rezervasyon_Cakisma_BU
BEFORE UPDATE ON Rezervasyon
FOR EACH ROW
BEGIN
    DECLARE v_cakisan INT UNSIGNED DEFAULT NULL;
    DECLARE v_kapasite TINYINT UNSIGNED;
    DECLARE v_mesaj VARCHAR(255);

    -- İptal edilmiş veya tamamlanmış rezervasyon yeniden açılamaz
    IF OLD.Durum IN ('IPTAL','TAMAMLANDI') AND NEW.Durum <> OLD.Durum THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'İptal edilmiş veya tamamlanmış rezervasyonun durumu değiştirilemez.';
    END IF;

    -- Kişi sayısı veya oda değiştiyse kapasiteyi yeniden kontrol et
    IF NEW.KisiSayisi <> OLD.KisiSayisi OR NEW.OdaID <> OLD.OdaID THEN
        SELECT ot.Kapasite INTO v_kapasite
          FROM Oda o JOIN OdaTipi ot ON ot.OdaTipiID = o.OdaTipiID
         WHERE o.OdaID = NEW.OdaID;
        IF NEW.KisiSayisi > v_kapasite THEN
            SET v_mesaj = CONCAT('Kişi sayısı oda kapasitesini aşıyor (kapasite: ', v_kapasite, ').');
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_mesaj;
        END IF;
    END IF;

    -- Oda veya tarihler değiştiyse çakışmayı yeniden kontrol et
    IF NEW.Durum IN ('BEKLEMEDE','ONAYLI')
       AND (NEW.OdaID <> OLD.OdaID
            OR NEW.GirisTarihi <> OLD.GirisTarihi
            OR NEW.CikisTarihi <> OLD.CikisTarihi) THEN
        SELECT r.RezervasyonID INTO v_cakisan
          FROM Rezervasyon r
         WHERE r.OdaID = NEW.OdaID
           AND r.RezervasyonID <> NEW.RezervasyonID
           AND r.Durum IN ('BEKLEMEDE','ONAYLI')
           AND r.GirisTarihi < NEW.CikisTarihi
           AND NEW.GirisTarihi < r.CikisTarihi
         LIMIT 1;

        IF v_cakisan IS NOT NULL THEN
            SET v_mesaj = CONCAT('Oda bu tarihlerde dolu: #', v_cakisan,
                                 ' numaralı rezervasyonla çakışıyor.');
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_mesaj;
        END IF;
    END IF;
END $$


-- ---------------------------------------------------------------------
-- T2-a  Check-in (Konaklama INSERT) -> oda DOLU
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_Konaklama_CheckIn_AI $$
CREATE TRIGGER trg_Konaklama_CheckIn_AI
AFTER INSERT ON Konaklama
FOR EACH ROW
BEGIN
    IF NEW.GercekCikis IS NULL THEN
        UPDATE Oda o
          JOIN Rezervasyon r ON r.OdaID = o.OdaID
           SET o.Durum = 'DOLU'
         WHERE r.RezervasyonID = NEW.RezervasyonID;
    END IF;
END $$

-- ---------------------------------------------------------------------
-- T2-b  Check-out (GercekCikis doldurulur) -> oda TEMIZLIKTE
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_Konaklama_CheckOut_AU $$
CREATE TRIGGER trg_Konaklama_CheckOut_AU
AFTER UPDATE ON Konaklama
FOR EACH ROW
BEGIN
    IF OLD.GercekCikis IS NULL AND NEW.GercekCikis IS NOT NULL THEN
        UPDATE Oda o
          JOIN Rezervasyon r ON r.OdaID = o.OdaID
           SET o.Durum = 'TEMIZLIKTE'
         WHERE r.RezervasyonID = NEW.RezervasyonID;
    END IF;
END $$

-- ---------------------------------------------------------------------
-- T2-c  Temizlik tamamlandı -> oda tekrar BOS (yalnızca TEMIZLIKTE ise)
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_Temizlik_Tamamlandi_AI $$
CREATE TRIGGER trg_Temizlik_Tamamlandi_AI
AFTER INSERT ON TemizlikKaydi
FOR EACH ROW
BEGIN
    IF NEW.Durum = 'TAMAMLANDI' THEN
        UPDATE Oda SET Durum = 'BOS'
         WHERE OdaID = NEW.OdaID AND Durum = 'TEMIZLIKTE';
    END IF;
END $$


-- ---------------------------------------------------------------------
-- T3  IslemLog tetikleyicileri: Rezervasyon
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_Rezervasyon_Log_AI $$
CREATE TRIGGER trg_Rezervasyon_Log_AI
AFTER INSERT ON Rezervasyon
FOR EACH ROW
BEGIN
    INSERT INTO IslemLog (TabloAdi, IslemTuru, KayitID, EskiDeger, YeniDeger)
    VALUES ('Rezervasyon', 'INSERT', NEW.RezervasyonID, NULL,
            JSON_OBJECT('MisafirID', NEW.MisafirID, 'OdaID', NEW.OdaID,
                        'GirisTarihi', NEW.GirisTarihi, 'CikisTarihi', NEW.CikisTarihi,
                        'KisiSayisi', NEW.KisiSayisi, 'Durum', NEW.Durum));
END $$

DROP TRIGGER IF EXISTS trg_Rezervasyon_Log_AU $$
CREATE TRIGGER trg_Rezervasyon_Log_AU
AFTER UPDATE ON Rezervasyon
FOR EACH ROW
BEGIN
    INSERT INTO IslemLog (TabloAdi, IslemTuru, KayitID, EskiDeger, YeniDeger)
    VALUES ('Rezervasyon', 'UPDATE', NEW.RezervasyonID,
            JSON_OBJECT('MisafirID', OLD.MisafirID, 'OdaID', OLD.OdaID,
                        'GirisTarihi', OLD.GirisTarihi, 'CikisTarihi', OLD.CikisTarihi,
                        'KisiSayisi', OLD.KisiSayisi, 'Durum', OLD.Durum),
            JSON_OBJECT('MisafirID', NEW.MisafirID, 'OdaID', NEW.OdaID,
                        'GirisTarihi', NEW.GirisTarihi, 'CikisTarihi', NEW.CikisTarihi,
                        'KisiSayisi', NEW.KisiSayisi, 'Durum', NEW.Durum));
END $$

DROP TRIGGER IF EXISTS trg_Rezervasyon_Log_AD $$
CREATE TRIGGER trg_Rezervasyon_Log_AD
AFTER DELETE ON Rezervasyon
FOR EACH ROW
BEGIN
    INSERT INTO IslemLog (TabloAdi, IslemTuru, KayitID, EskiDeger, YeniDeger)
    VALUES ('Rezervasyon', 'DELETE', OLD.RezervasyonID,
            JSON_OBJECT('MisafirID', OLD.MisafirID, 'OdaID', OLD.OdaID,
                        'GirisTarihi', OLD.GirisTarihi, 'CikisTarihi', OLD.CikisTarihi,
                        'KisiSayisi', OLD.KisiSayisi, 'Durum', OLD.Durum),
            NULL);
END $$

-- ---------------------------------------------------------------------
-- T3  IslemLog tetikleyicileri: Odeme
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_Odeme_Log_AI $$
CREATE TRIGGER trg_Odeme_Log_AI
AFTER INSERT ON Odeme
FOR EACH ROW
BEGIN
    INSERT INTO IslemLog (TabloAdi, IslemTuru, KayitID, EskiDeger, YeniDeger)
    VALUES ('Odeme', 'INSERT', NEW.OdemeID, NULL,
            JSON_OBJECT('KonaklamaID', NEW.KonaklamaID, 'Tutar', NEW.Tutar,
                        'OdemeTuru', NEW.OdemeTuru, 'OdemeTarihi', NEW.OdemeTarihi,
                        'PersonelID', NEW.PersonelID));
END $$

DROP TRIGGER IF EXISTS trg_Odeme_Log_AU $$
CREATE TRIGGER trg_Odeme_Log_AU
AFTER UPDATE ON Odeme
FOR EACH ROW
BEGIN
    INSERT INTO IslemLog (TabloAdi, IslemTuru, KayitID, EskiDeger, YeniDeger)
    VALUES ('Odeme', 'UPDATE', NEW.OdemeID,
            JSON_OBJECT('KonaklamaID', OLD.KonaklamaID, 'Tutar', OLD.Tutar,
                        'OdemeTuru', OLD.OdemeTuru, 'OdemeTarihi', OLD.OdemeTarihi),
            JSON_OBJECT('KonaklamaID', NEW.KonaklamaID, 'Tutar', NEW.Tutar,
                        'OdemeTuru', NEW.OdemeTuru, 'OdemeTarihi', NEW.OdemeTarihi));
END $$

DROP TRIGGER IF EXISTS trg_Odeme_Log_AD $$
CREATE TRIGGER trg_Odeme_Log_AD
AFTER DELETE ON Odeme
FOR EACH ROW
BEGIN
    INSERT INTO IslemLog (TabloAdi, IslemTuru, KayitID, EskiDeger, YeniDeger)
    VALUES ('Odeme', 'DELETE', OLD.OdemeID,
            JSON_OBJECT('KonaklamaID', OLD.KonaklamaID, 'Tutar', OLD.Tutar,
                        'OdemeTuru', OLD.OdemeTuru, 'OdemeTarihi', OLD.OdemeTarihi),
            NULL);
END $$


-- ---------------------------------------------------------------------
-- Ek: aynı oda tipi için sezon aralıkları çakışamaz
--   (aksi halde bir gecenin fiyatı belirsiz olurdu)
--   Sezon bitiş tarihi DAHİL olduğu için <= kullanılır.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_SezonFiyati_Cakisma_BI $$
CREATE TRIGGER trg_SezonFiyati_Cakisma_BI
BEFORE INSERT ON SezonFiyati
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM SezonFiyati s
                WHERE s.OdaTipiID = NEW.OdaTipiID
                  AND s.BaslangicTarihi <= NEW.BitisTarihi
                  AND NEW.BaslangicTarihi <= s.BitisTarihi) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Bu oda tipi için seçilen tarihlerle çakışan bir sezon fiyatı zaten var.';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_SezonFiyati_Cakisma_BU $$
CREATE TRIGGER trg_SezonFiyati_Cakisma_BU
BEFORE UPDATE ON SezonFiyati
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM SezonFiyati s
                WHERE s.OdaTipiID = NEW.OdaTipiID
                  AND s.SezonFiyatiID <> NEW.SezonFiyatiID
                  AND s.BaslangicTarihi <= NEW.BitisTarihi
                  AND NEW.BaslangicTarihi <= s.BitisTarihi) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Bu oda tipi için seçilen tarihlerle çakışan bir sezon fiyatı zaten var.';
    END IF;
END $$

DELIMITER ;
