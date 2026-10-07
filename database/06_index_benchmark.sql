-- =====================================================================
--  06 - İNDEKS PERFORMANS KARŞILAŞTIRMASI
--  Aynı yapıda iki büyük kopya tablo üretilir:
--     bench_*_indekssiz : yalnızca PRIMARY KEY
--     bench_*_indeksli  : rapordaki indekslerle
--  Her sorgu 30 kez çalıştırılır, ortalama süre (ms) raporlanır.
--  Çalıştırma: proje klasöründe
--    mysql --default-character-set=utf8mb4 -u root -p -e "source database/06_index_benchmark.sql"
-- =====================================================================
SET NAMES utf8mb4;
USE otel_db;
SET SESSION cte_max_recursion_depth = 1000000;

DROP TABLE IF EXISTS bench_rez_indekssiz, bench_rez_indeksli,
                     bench_misafir_indekssiz, bench_misafir_indeksli,
                     bench_oda_indekssiz, bench_oda_indeksli, bench_sonuc;

-- ------------------------------------------------ 500.000 rezervasyon
CREATE TABLE bench_rez_indekssiz (
    RezervasyonID INT UNSIGNED PRIMARY KEY,
    OdaID INT UNSIGNED NOT NULL, MisafirID INT UNSIGNED NOT NULL,
    GirisTarihi DATE NOT NULL, CikisTarihi DATE NOT NULL,
    Durum ENUM('BEKLEMEDE','ONAYLI','IPTAL','TAMAMLANDI') NOT NULL
) ENGINE=InnoDB;

INSERT INTO bench_rez_indekssiz
WITH RECURSIVE n (i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 500000)
SELECT i,
       1 + (i % 1000),
       1 + (i * 7919) % 200000,
       DATE '2018-01-01' + INTERVAL ((i DIV 1000) * 7 + (i % 3)) DAY,
       DATE '2018-01-01' + INTERVAL ((i DIV 1000) * 7 + (i % 3) + 1 + (i % 5)) DAY,
       ELT(1 + (i % 4), 'BEKLEMEDE','ONAYLI','IPTAL','TAMAMLANDI')
  FROM n;

CREATE TABLE bench_rez_indeksli LIKE bench_rez_indekssiz;
INSERT INTO bench_rez_indeksli SELECT * FROM bench_rez_indekssiz;
CREATE INDEX ix_b_oda_tarih ON bench_rez_indeksli (OdaID, GirisTarihi, CikisTarihi);
CREATE INDEX ix_b_giris     ON bench_rez_indeksli (GirisTarihi);
CREATE INDEX ix_b_cikis     ON bench_rez_indeksli (CikisTarihi);

-- ------------------------------------------------ 200.000 misafir
CREATE TABLE bench_misafir_indekssiz (
    MisafirID INT UNSIGNED PRIMARY KEY,
    Ad VARCHAR(50), Soyad VARCHAR(50), Eposta VARCHAR(120) NOT NULL
) ENGINE=InnoDB;
INSERT INTO bench_misafir_indekssiz
WITH RECURSIVE n (i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 200000)
SELECT i, CONCAT('Ad', i), CONCAT('Soyad', i), CONCAT('misafir', i, '@example.com') FROM n;
CREATE TABLE bench_misafir_indeksli LIKE bench_misafir_indekssiz;
INSERT INTO bench_misafir_indeksli SELECT * FROM bench_misafir_indekssiz;
CREATE UNIQUE INDEX ix_b_eposta ON bench_misafir_indeksli (Eposta);

-- ------------------------------------------------ 100.000 oda (zincir otel senaryosu)
CREATE TABLE bench_oda_indekssiz (
    OdaID INT UNSIGNED PRIMARY KEY, OdaNo VARCHAR(10) NOT NULL, Kat TINYINT
) ENGINE=InnoDB;
INSERT INTO bench_oda_indekssiz
WITH RECURSIVE n (i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 100000)
SELECT i, CONCAT('R', LPAD(i, 6, '0')), i % 50 FROM n;
CREATE TABLE bench_oda_indeksli LIKE bench_oda_indekssiz;
INSERT INTO bench_oda_indeksli SELECT * FROM bench_oda_indekssiz;
CREATE UNIQUE INDEX ix_b_odano ON bench_oda_indeksli (OdaNo);

ANALYZE TABLE bench_rez_indekssiz, bench_rez_indeksli, bench_misafir_indekssiz,
              bench_misafir_indeksli, bench_oda_indekssiz, bench_oda_indeksli;

CREATE TABLE bench_sonuc (
    Senaryo VARCHAR(80), Tablo VARCHAR(20), TekrarSayisi INT, OrtalamaMs DECIMAL(10,3)
);

-- ------------------------------------------------ ölçüm prosedürü
DROP PROCEDURE IF EXISTS sp_Benchmark;
DELIMITER $$
CREATE PROCEDURE sp_Benchmark(IN p_senaryo VARCHAR(80), IN p_tablo VARCHAR(20),
                              IN p_sql TEXT, IN p_tekrar INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE t0 DATETIME(6);
    SET @q = p_sql;
    PREPARE st FROM @q;
    EXECUTE st;                         -- ısınma turu (veri belleğe yüklensin)
    SET t0 = SYSDATE(6);
    WHILE i < p_tekrar DO
        EXECUTE st;
        SET i = i + 1;
    END WHILE;
    INSERT INTO bench_sonuc VALUES
        (p_senaryo, p_tablo, p_tekrar, TIMESTAMPDIFF(MICROSECOND, t0, SYSDATE(6)) / 1000 / p_tekrar);
    DEALLOCATE PREPARE st;
END $$
DELIMITER ;

-- S1: çakışma kontrolü (trigger'ın sorgusu)
CALL sp_Benchmark('S1 Çakışma kontrolü (oda + tarih aralığı)', 'indekssiz',
 'SELECT COUNT(*) INTO @x FROM bench_rez_indekssiz WHERE OdaID = 517 AND Durum IN (''BEKLEMEDE'',''ONAYLI'') AND GirisTarihi < ''2024-06-10'' AND ''2024-06-05'' < CikisTarihi', 30);
CALL sp_Benchmark('S1 Çakışma kontrolü (oda + tarih aralığı)', 'indeksli',
 'SELECT COUNT(*) INTO @x FROM bench_rez_indeksli WHERE OdaID = 517 AND Durum IN (''BEKLEMEDE'',''ONAYLI'') AND GirisTarihi < ''2024-06-10'' AND ''2024-06-05'' < CikisTarihi', 30);

-- S2: belirli günün girişleri
CALL sp_Benchmark('S2 Belirli gün girişleri (GirisTarihi = ?)', 'indekssiz',
 'SELECT COUNT(*) INTO @x FROM bench_rez_indekssiz WHERE GirisTarihi = ''2024-06-05''', 30);
CALL sp_Benchmark('S2 Belirli gün girişleri (GirisTarihi = ?)', 'indeksli',
 'SELECT COUNT(*) INTO @x FROM bench_rez_indeksli WHERE GirisTarihi = ''2024-06-05''', 30);

-- S3: haftalık çıkış listesi
CALL sp_Benchmark('S3 Haftalık çıkışlar (CikisTarihi BETWEEN)', 'indekssiz',
 'SELECT COUNT(*) INTO @x FROM bench_rez_indekssiz WHERE CikisTarihi BETWEEN ''2024-06-01'' AND ''2024-06-07''', 30);
CALL sp_Benchmark('S3 Haftalık çıkışlar (CikisTarihi BETWEEN)', 'indeksli',
 'SELECT COUNT(*) INTO @x FROM bench_rez_indeksli WHERE CikisTarihi BETWEEN ''2024-06-01'' AND ''2024-06-07''', 30);

-- S4: misafir e-posta araması
CALL sp_Benchmark('S4 Misafir e-posta araması', 'indekssiz',
 'SELECT MisafirID INTO @x FROM bench_misafir_indekssiz WHERE Eposta = ''misafir154321@example.com''', 30);
CALL sp_Benchmark('S4 Misafir e-posta araması', 'indeksli',
 'SELECT MisafirID INTO @x FROM bench_misafir_indeksli WHERE Eposta = ''misafir154321@example.com''', 30);

-- S5: oda numarası araması
CALL sp_Benchmark('S5 Oda numarası araması', 'indekssiz',
 'SELECT OdaID INTO @x FROM bench_oda_indekssiz WHERE OdaNo = ''R087654''', 30);
CALL sp_Benchmark('S5 Oda numarası araması', 'indeksli',
 'SELECT OdaID INTO @x FROM bench_oda_indeksli WHERE OdaNo = ''R087654''', 30);

-- ------------------------------------------------ SONUÇ TABLOSU
SELECT a.Senaryo,
       a.OrtalamaMs AS IndekssizMs,
       b.OrtalamaMs AS IndeksliMs,
       ROUND(a.OrtalamaMs / NULLIF(b.OrtalamaMs, 0), 1) AS HizlanmaKat
  FROM bench_sonuc a
  JOIN bench_sonuc b ON b.Senaryo = a.Senaryo AND b.Tablo = 'indeksli'
 WHERE a.Tablo = 'indekssiz'
 ORDER BY a.Senaryo;

-- ------------------------------------------------ YÜRÜTME PLANLARI
EXPLAIN SELECT COUNT(*) FROM bench_rez_indekssiz
 WHERE OdaID = 517 AND GirisTarihi < '2024-06-10' AND '2024-06-05' < CikisTarihi;
EXPLAIN SELECT COUNT(*) FROM bench_rez_indeksli
 WHERE OdaID = 517 AND GirisTarihi < '2024-06-10' AND '2024-06-05' < CikisTarihi;