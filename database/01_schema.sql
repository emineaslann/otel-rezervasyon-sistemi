-- =====================================================================
--  Otel Rezervasyon ve Konaklama Yönetim Sistemi
--  01 - ŞEMA (tablolar, kısıtlar, indeksler)
-- =====================================================================
SET NAMES utf8mb4;

DROP DATABASE IF EXISTS otel_db;
CREATE DATABASE otel_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_turkish_ci;
USE otel_db;

-- ---------------------------------------------------------------------
-- OdaTipi: standart, deluxe, suit ...
-- ---------------------------------------------------------------------
CREATE TABLE OdaTipi (
    OdaTipiID   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    TipAdi      VARCHAR(50)      NOT NULL,
    Kapasite    TINYINT UNSIGNED NOT NULL,
    Aciklama    VARCHAR(500)     NULL,
    TemelFiyat  DECIMAL(10,2)    NOT NULL,
    CONSTRAINT uq_odatipi_ad       UNIQUE (TipAdi),
    CONSTRAINT ck_odatipi_kapasite CHECK (Kapasite BETWEEN 1 AND 10),
    CONSTRAINT ck_odatipi_fiyat    CHECK (TemelFiyat > 0)
) ENGINE=InnoDB;


-- ---------------------------------------------------------------------
-- Oda
-- ---------------------------------------------------------------------
CREATE TABLE Oda (
    OdaID      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    OdaNo      VARCHAR(10)  NOT NULL,
    Kat        TINYINT      NOT NULL,
    OdaTipiID  INT UNSIGNED NOT NULL,
    Durum      ENUM('BOS','DOLU','TEMIZLIKTE','BAKIMDA') NOT NULL DEFAULT 'BOS',
    CONSTRAINT uq_oda_no  UNIQUE (OdaNo),
    CONSTRAINT fk_oda_tip FOREIGN KEY (OdaTipiID) REFERENCES OdaTipi(OdaTipiID)
                          ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_oda_kat CHECK (Kat BETWEEN -2 AND 50)
) ENGINE=InnoDB;
CREATE INDEX ix_oda_tip_durum ON Oda (OdaTipiID, Durum);


-- ---------------------------------------------------------------------
-- SezonFiyati: oda tipi + tarih aralığı -> gecelik fiyat
-- (Aynı oda tipi için sezonların çakışması tetikleyici ile engellenecek)
-- ---------------------------------------------------------------------
CREATE TABLE SezonFiyati (
    SezonFiyatiID   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    OdaTipiID       INT UNSIGNED  NOT NULL,
    SezonAdi        VARCHAR(50)   NOT NULL,
    BaslangicTarihi DATE          NOT NULL,
    BitisTarihi     DATE          NOT NULL,
    GecelikFiyat    DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_sezon_tip   FOREIGN KEY (OdaTipiID) REFERENCES OdaTipi(OdaTipiID)
                              ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_sezon_tarih CHECK (BitisTarihi >= BaslangicTarihi),
    CONSTRAINT ck_sezon_fiyat CHECK (GecelikFiyat > 0)
) ENGINE=InnoDB;
CREATE INDEX ix_sezon_tip_tarih ON SezonFiyati (OdaTipiID, BaslangicTarihi, BitisTarihi);


-- ---------------------------------------------------------------------
-- Misafir
-- ---------------------------------------------------------------------
CREATE TABLE Misafir (
    MisafirID    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    Ad           VARCHAR(50)  NOT NULL,
    Soyad        VARCHAR(50)  NOT NULL,
    KimlikNo     VARCHAR(20)  NOT NULL COMMENT 'TC kimlik veya pasaport no',
    Telefon      VARCHAR(20)  NULL,
    Eposta       VARCHAR(120) NOT NULL,
    DogumTarihi  DATE         NULL,
    KayitTarihi  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_misafir_kimlik UNIQUE (KimlikNo),
    CONSTRAINT uq_misafir_eposta UNIQUE (Eposta),
    CONSTRAINT ck_misafir_eposta CHECK (Eposta LIKE '%_@_%._%')
) ENGINE=InnoDB;
CREATE INDEX ix_misafir_adsoyad ON Misafir (Soyad, Ad);


-- ---------------------------------------------------------------------
-- Personel
-- ---------------------------------------------------------------------
CREATE TABLE Personel (
    PersonelID     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    Ad             VARCHAR(50)  NOT NULL,
    Soyad          VARCHAR(50)  NOT NULL,
    Gorev          ENUM('RESEPSIYON','KAT_GOREVLISI','YONETICI') NOT NULL,
    IseGirisTarihi DATE         NOT NULL,
    Telefon        VARCHAR(20)  NULL,
    Aktif          BOOLEAN      NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;


-- ---------------------------------------------------------------------
-- Kullanici: giriş bilgileri + rol; misafir VEYA personele bağlıdır
-- ---------------------------------------------------------------------
CREATE TABLE Kullanici (
    KullaniciID     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    KullaniciAdi    VARCHAR(120) NOT NULL,
    SifreHash       VARCHAR(255) NOT NULL,
    Rol             ENUM('MISAFIR','RESEPSIYON','YONETICI') NOT NULL,
    MisafirID       INT UNSIGNED NULL,
    PersonelID      INT UNSIGNED NULL,
    Aktif           BOOLEAN  NOT NULL DEFAULT TRUE,
    OlusturmaTarihi DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    SonGiris        DATETIME NULL,
    CONSTRAINT uq_kullanici_ad       UNIQUE (KullaniciAdi),
    CONSTRAINT uq_kullanici_misafir  UNIQUE (MisafirID),
    CONSTRAINT uq_kullanici_personel UNIQUE (PersonelID),
    CONSTRAINT fk_kullanici_misafir  FOREIGN KEY (MisafirID)  REFERENCES Misafir(MisafirID),
    CONSTRAINT fk_kullanici_personel FOREIGN KEY (PersonelID) REFERENCES Personel(PersonelID),
    -- misafir rolü yalnızca misafire, personel rolleri yalnızca personele bağlanır
    CONSTRAINT ck_kullanici_baglanti CHECK (
        (Rol = 'MISAFIR'  AND MisafirID IS NOT NULL AND PersonelID IS NULL) OR
        (Rol <> 'MISAFIR' AND PersonelID IS NOT NULL AND MisafirID IS NULL)
    )
) ENGINE=InnoDB;


-- ---------------------------------------------------------------------
-- Rezervasyon
--   GirisTarihi dahil, CikisTarihi hariç: [Giris, Cikis)
--   Gece sayısı = CikisTarihi - GirisTarihi
-- ---------------------------------------------------------------------
CREATE TABLE Rezervasyon (
    RezervasyonID   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    MisafirID       INT UNSIGNED NOT NULL,
    OdaID           INT UNSIGNED NOT NULL,
    GirisTarihi     DATE         NOT NULL,
    CikisTarihi     DATE         NOT NULL,
    KisiSayisi      TINYINT UNSIGNED NOT NULL,
    Durum           ENUM('BEKLEMEDE','ONAYLI','IPTAL','TAMAMLANDI') NOT NULL DEFAULT 'BEKLEMEDE',
    OlusturmaTarihi DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rez_misafir FOREIGN KEY (MisafirID) REFERENCES Misafir(MisafirID),
    CONSTRAINT fk_rez_oda     FOREIGN KEY (OdaID)     REFERENCES Oda(OdaID),
    CONSTRAINT ck_rez_tarih   CHECK (CikisTarihi > GirisTarihi),
    CONSTRAINT ck_rez_kisi    CHECK (KisiSayisi >= 1)
) ENGINE=InnoDB;
-- Rezervasyon tarihleri üzerinde indeksler (hocanın istediği)
CREATE INDEX ix_rez_oda_tarih ON Rezervasyon (OdaID, GirisTarihi, CikisTarihi);
CREATE INDEX ix_rez_giris     ON Rezervasyon (GirisTarihi);
CREATE INDEX ix_rez_cikis     ON Rezervasyon (CikisTarihi);
CREATE INDEX ix_rez_misafir   ON Rezervasyon (MisafirID, GirisTarihi);
CREATE INDEX ix_rez_durum     ON Rezervasyon (Durum);


-- ---------------------------------------------------------------------
-- Konaklama: rezervasyonun fiilen gerçekleşmesi (check-in / check-out)
-- ---------------------------------------------------------------------
CREATE TABLE Konaklama (
    KonaklamaID      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    RezervasyonID    INT UNSIGNED NOT NULL,
    GercekGiris      DATETIME     NOT NULL,
    GercekCikis      DATETIME     NULL,
    GirisPersonelID  INT UNSIGNED NOT NULL COMMENT 'check-in yapan personel',
    CikisPersonelID  INT UNSIGNED NULL     COMMENT 'check-out yapan personel',
    CONSTRAINT uq_konaklama_rez    UNIQUE (RezervasyonID),
    CONSTRAINT fk_konaklama_rez    FOREIGN KEY (RezervasyonID)   REFERENCES Rezervasyon(RezervasyonID),
    CONSTRAINT fk_konaklama_girisp FOREIGN KEY (GirisPersonelID) REFERENCES Personel(PersonelID),
    CONSTRAINT fk_konaklama_cikisp FOREIGN KEY (CikisPersonelID) REFERENCES Personel(PersonelID),
    CONSTRAINT ck_konaklama_zaman  CHECK (GercekCikis IS NULL OR GercekCikis >= GercekGiris)
) ENGINE=InnoDB;
CREATE INDEX ix_konaklama_cikis ON Konaklama (GercekCikis);


-- ---------------------------------------------------------------------
-- EkHizmet: kahvaltı, spa, otopark, oda servisi ...
-- ---------------------------------------------------------------------
CREATE TABLE EkHizmet (
    EkHizmetID  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    HizmetAdi   VARCHAR(80)   NOT NULL,
    BirimFiyat  DECIMAL(10,2) NOT NULL,
    Aktif       BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_ekhizmet_ad    UNIQUE (HizmetAdi),
    CONSTRAINT ck_ekhizmet_fiyat CHECK (BirimFiyat >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- KonaklamaHizmeti: konaklama süresince alınan ek hizmetler (N-N ara tablo)
--   BirimFiyat: satış anındaki fiyat. EkHizmet fiyatı sonradan değişse
--   bile geçmiş faturalar bozulmaz.
-- ---------------------------------------------------------------------
CREATE TABLE KonaklamaHizmeti (
    KonaklamaHizmetiID INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    KonaklamaID        INT UNSIGNED      NOT NULL,
    EkHizmetID         INT UNSIGNED      NOT NULL,
    Adet               SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    BirimFiyat         DECIMAL(10,2)     NOT NULL,
    Tarih              DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kh_konaklama FOREIGN KEY (KonaklamaID) REFERENCES Konaklama(KonaklamaID),
    CONSTRAINT fk_kh_hizmet    FOREIGN KEY (EkHizmetID)  REFERENCES EkHizmet(EkHizmetID),
    CONSTRAINT ck_kh_adet      CHECK (Adet >= 1)
) ENGINE=InnoDB;


-- ---------------------------------------------------------------------
-- Odeme
-- ---------------------------------------------------------------------
CREATE TABLE Odeme (
    OdemeID      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    KonaklamaID  INT UNSIGNED  NOT NULL,
    Tutar        DECIMAL(10,2) NOT NULL,
    OdemeTuru    ENUM('NAKIT','KART') NOT NULL,
    OdemeTarihi  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PersonelID   INT UNSIGNED  NULL COMMENT 'ödemeyi alan personel',
    CONSTRAINT fk_odeme_konaklama FOREIGN KEY (KonaklamaID) REFERENCES Konaklama(KonaklamaID),
    CONSTRAINT fk_odeme_personel  FOREIGN KEY (PersonelID)  REFERENCES Personel(PersonelID),
    CONSTRAINT ck_odeme_tutar     CHECK (Tutar > 0)
) ENGINE=InnoDB;
CREATE INDEX ix_odeme_tarih ON Odeme (OdemeTarihi);

-- ---------------------------------------------------------------------
-- TemizlikKaydi
-- ---------------------------------------------------------------------
CREATE TABLE TemizlikKaydi (
    TemizlikKaydiID INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    OdaID           INT UNSIGNED NOT NULL,
    PersonelID      INT UNSIGNED NOT NULL,
    Tarih           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Durum           ENUM('BASLADI','TAMAMLANDI') NOT NULL DEFAULT 'TAMAMLANDI',
    Notlar          VARCHAR(255) NULL,
    CONSTRAINT fk_temizlik_oda      FOREIGN KEY (OdaID)      REFERENCES Oda(OdaID),
    CONSTRAINT fk_temizlik_personel FOREIGN KEY (PersonelID) REFERENCES Personel(PersonelID)
) ENGINE=InnoDB;
CREATE INDEX ix_temizlik_oda_tarih ON TemizlikKaydi (OdaID, Tarih);


-- ---------------------------------------------------------------------
-- IslemLog: tetikleyiciler tarafından doldurulan işlem geçmişi
-- ---------------------------------------------------------------------
CREATE TABLE IslemLog (
    LogID       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    TabloAdi    VARCHAR(50)  NOT NULL,
    IslemTuru   ENUM('INSERT','UPDATE','DELETE') NOT NULL,
    KayitID     INT UNSIGNED NOT NULL,
    EskiDeger   JSON NULL,
    YeniDeger   JSON NULL,
    IslemZamani DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    DbKullanici VARCHAR(100) NOT NULL DEFAULT (CURRENT_USER())
) ENGINE=InnoDB;
CREATE INDEX ix_log_tablo_kayit ON IslemLog (TabloAdi, KayitID);
CREATE INDEX ix_log_zaman       ON IslemLog (IslemZamani);