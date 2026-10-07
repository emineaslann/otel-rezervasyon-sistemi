-- =====================================================================
--  Otel Rezervasyon Sistemi — TAM KURULUM
--  Proje ana klasöründeyken çalıştırın:
--    mysql --default-character-set=utf8mb4 -u root -p -e "source database/00_hepsini_kur.sql"
--  DİKKAT: otel_db veritabanını silip sıfırdan kurar.
-- =====================================================================
SOURCE database/01_schema.sql;
SOURCE database/02_triggers.sql;
SOURCE database/03_procedures.sql;
SOURCE database/04_views.sql;
SOURCE database/05_seed.sql;

SELECT 'Kurulum tamamlandı' AS Durum;