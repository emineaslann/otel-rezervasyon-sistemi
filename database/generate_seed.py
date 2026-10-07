"""
Örnek veri üreticisi -> 05_seed.sql

Tarihler CURDATE()'e göre GÖRELİ üretilir (@bugun + INTERVAL n DAY).
Betik hangi gün çalıştırılırsa çalıştırılsın "bugün giriş/çıkış",
otelde kalanlar, geçmiş ve gelecek rezervasyonlar tutarlı olur.

Kullanım:  python database/generate_seed.py
"""
import random
from pathlib import Path

random.seed(2026)                     # her çalıştırmada aynı veri üretilsin
OUT = Path(__file__).with_name("05_seed.sql")

# BCrypt özetleri ($2a$, Spring Security uyumlu)
#   Yonetici123!  /  Resepsiyon123!  /  Misafir123!
HASH_YONETICI = "$2a$10$1uId6H9od/XHxekP2LslHOmW0rBhNg86huxr1LSzi.6.7QJONfAR2"
HASH_RESEPSIYON = "$2a$10$C4RD8JeFbtfdoKUt7Nv8XuHYHegejXlGG4nQkVtg2T6GO0j4yyVEu"
HASH_MISAFIR = "$2a$10$Wxn/kvG3fsBwujAf5gNaduQlGcVEEUrir/5hkahxcthgRqv/TcrLK"

# ----------------------------------------------------------------- sabitler
ODA_TIPLERI = [
    (1, "Standart", 2, "Çift veya iki tek kişilik yatak, 24 m², şehir manzarası", 2400),
    (2, "Deluxe", 3, "Geniş yatak + çekyat, 32 m², balkon ve deniz manzarası", 3600),
    (3, "Aile", 4, "İki yatak odası, 42 m², çocuklar için ek yatak imkânı", 4800),
    (4, "Suit", 4, "Ayrı oturma odası, 58 m², jakuzi ve panoramik deniz manzarası", 7200),
]
# Kasım 1 - Aralık 19 bilinçli olarak tanımsız -> TemelFiyat kullanılır
SEZONLAR = [
    ("Kış", "01-01", "03-31", 0.85),
    ("İlkbahar", "04-01", "05-31", 1.00),
    ("Yaz", "06-01", "08-31", 1.45),
    ("Sonbahar", "09-01", "10-31", 1.10),
    ("Yılbaşı", "12-20", "12-31", 1.60),
]
YILLAR = [2025, 2026, 2027, 2028]

# 4 kat x 8 oda = 32 oda
ODALAR = []
oda_id = 1
for kat in range(1, 5):
    for sira in range(1, 9):
        if kat <= 2:
            tip = 1 if sira <= 6 else 2
        elif kat == 3:
            tip = 2 if sira <= 5 else 3
        else:
            tip = 3 if sira <= 4 else 4
        ODALAR.append((oda_id, f"{kat}{sira:02d}", kat, tip))
        oda_id += 1
KAPASITE = {t[0]: t[2] for t in ODA_TIPLERI}

EK_HIZMETLER = [
    (1, "Açık büfe kahvaltı", 350), (2, "Akşam yemeği", 650), (3, "Spa ve masaj", 1200),
    (4, "Otopark (gecelik)", 200), (5, "Oda servisi", 450), (6, "Çamaşırhane", 300),
    (7, "Minibar", 250), (8, "Havalimanı transferi", 900),
]

PERSONEL = [
    (1, "Selin", "Karaca", "YONETICI", "2019-03-01", "05321110001"),
    (2, "Emre", "Aydın", "RESEPSIYON", "2021-06-15", "05321110002"),
    (3, "Zeynep", "Kılıç", "RESEPSIYON", "2022-02-01", "05321110003"),
    (4, "Burak", "Şahin", "RESEPSIYON", "2023-09-10", "05321110004"),
    (5, "Elif", "Doğan", "RESEPSIYON", "2024-04-22", "05321110005"),
    (6, "Hatice", "Yıldız", "KAT_GOREVLISI", "2020-05-05", "05321110006"),
    (7, "Murat", "Öztürk", "KAT_GOREVLISI", "2021-11-01", "05321110007"),
    (8, "Fatma", "Arslan", "KAT_GOREVLISI", "2023-01-16", "05321110008"),
    (9, "Kemal", "Çelik", "KAT_GOREVLISI", "2024-07-01", "05321110009"),
]
RESEPSIYON = [2, 3, 4, 5]
KAT_GOREVLISI = [6, 7, 8, 9]

ADLAR = ["Ayşe", "Mehmet", "Fatma", "Ahmet", "Zeynep", "Mustafa", "Elif", "Ali", "Merve", "Hüseyin",
         "Esra", "Hasan", "Büşra", "İbrahim", "Selin", "Can", "Deniz", "Ece", "Oğuz", "Gizem",
         "Kerem", "Derya", "Serkan", "Pınar", "Tolga", "Seda", "Volkan", "Aslı", "Onur", "Tuğba"]
SOYADLAR = ["Yılmaz", "Kaya", "Demir", "Şahin", "Çelik", "Yıldız", "Yıldırım", "Öztürk", "Aydın",
            "Özdemir", "Arslan", "Doğan", "Kılıç", "Aslan", "Çetin", "Kara", "Koç", "Kurt", "Özkan",
            "Şimşek", "Polat", "Korkmaz", "Erdem", "Güneş", "Aksoy"]
YABANCI = [("Anna", "Schmidt", "DE"), ("Lukas", "Müller", "DE"), ("Olga", "Ivanova", "RU"),
           ("Dmitri", "Petrov", "RU"), ("Sara", "Haddad", "JO"), ("John", "Carter", "US"),
           ("Emma", "Wilson", "GB"), ("Nino", "Beridze", "GE")]
TR_MAP = str.maketrans("çğıöşüÇĞİÖŞÜ", "cgiosuCGIOSU")


def tc_kimlik_uret():
    """Algoritmaya uygun (geçerli formatta) rastgele TC kimlik no."""
    d = [random.randint(1, 9)] + [random.randint(0, 9) for _ in range(8)]
    d10 = ((d[0] + d[2] + d[4] + d[6] + d[8]) * 7 - (d[1] + d[3] + d[5] + d[7])) % 10
    d11 = (sum(d) + d10) % 10
    return "".join(map(str, d + [d10, d11]))


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, (int, float)):
        return str(v)
    return "'" + str(v).replace("'", "''") + "'"


def gun(n, saat=None):
    """@bugun'e göre göreli tarih ifadesi."""
    expr = f"(@bugun + INTERVAL {n} DAY)"
    return f"TIMESTAMP({expr}, '{saat}')" if saat else expr


def bloklar(tablo, kolonlar, satirlar, boyut=200):
    out = []
    for i in range(0, len(satirlar), boyut):
        parca = satirlar[i:i + boyut]
        out.append(f"INSERT INTO {tablo} ({', '.join(kolonlar)}) VALUES\n  "
                   + ",\n  ".join("(" + ", ".join(s) + ")" for s in parca) + ";")
    return "\n".join(out)


# ----------------------------------------------------------------- misafirler
misafirler = []
kullanilan = set()
for mid in range(1, 61):
    if mid <= 52:
        ad, soyad = random.choice(ADLAR), random.choice(SOYADLAR)
        kimlik = tc_kimlik_uret()
        tel = f"05{random.randint(30, 59)}{random.randint(1000000, 9999999)}"
    else:
        ad, soyad, ulke = YABANCI[mid - 53]
        kimlik = f"{ulke}{random.randint(1000000, 9999999)}"
        tel = f"+{random.randint(1, 99)}{random.randint(100000000, 999999999)}"
    base = f"{ad}.{soyad}".lower().translate(TR_MAP)
    eposta, k = f"{base}@example.com", 2
    while eposta in kullanilan:
        eposta, k = f"{base}{k}@example.com", k + 1
    kullanilan.add(eposta)
    dogum = f"{random.randint(1958, 2004)}-{random.randint(1, 12):02d}-{random.randint(1, 28):02d}"
    misafirler.append((mid, ad, soyad, kimlik, tel, eposta, dogum))
# demo misafiri sabit
misafirler[0] = (1, "Ayşe", "Yılmaz", "10000000146", "05551234567", "ayse.yilmaz@example.com", "1995-04-12")

# ----------------------------------------------------------------- rezervasyonlar
rezler, konaklamalar = [], []
rid = kid = 1
GECMIS, GELECEK = -300, 90
bugun_bekleyen = 0

for (oid, odano, kat, tip) in ODALAR:
    t = GECMIS + random.randint(0, 6)
    onceki_bugun = False
    while True:
        bosluk = random.choice([0, 1, 1, 2, 2, 3, 4, 5, 7, 10])
        if onceki_bugun:
            bosluk = max(bosluk, 1)
        bas = t + bosluk
        if t < 0 < bas and not onceki_bugun and random.random() < 0.5:
            bas = 0                                   # bugün giriş (demo için)
        gece = random.choices([1, 2, 3, 4, 5, 7], weights=[14, 26, 24, 16, 12, 8])[0]
        bit = bas + gece
        if bit > GELECEK:
            break
        kisi = random.randint(1, KAPASITE[tip])
        misafir = random.randint(1, 60)
        olus = bas - random.randint(1, 45)
        if olus > 0:
            olus = -random.randint(0, 10)
        olus_saat = f"{random.randint(8, 22):02d}:{random.randint(0, 59):02d}:00"

        konaklama = None
        if bit < 0:                                   # geçmiş
            durum = "IPTAL" if random.random() < 0.08 else "TAMAMLANDI"
            if durum == "TAMAMLANDI":
                konaklama = (bas, f"{random.randint(14, 19):02d}:{random.randint(0, 59):02d}:00",
                             bit, f"{random.randint(8, 11):02d}:{random.randint(0, 59):02d}:00")
        elif bas < 0 <= bit or (bas == 0 and bit > 0):  # otelde / bugün geliyor
            durum = "ONAYLI"
            if bas == 0 and random.random() < 0.7:
                bugun_bekleyen += 1                   # bugün giriş, check-in yok
            else:
                saat = f"{random.randint(14, 19):02d}:{random.randint(0, 59):02d}:00" if bas < 0 else None
                konaklama = (bas, saat, None, None)
        else:                                         # gelecek
            r = random.random()
            durum = "IPTAL" if r < 0.08 else ("BEKLEMEDE" if r < 0.30 else "ONAYLI")

        rezler.append((rid, misafir, oid, bas, bit, kisi, durum, olus, olus_saat))
        if konaklama:
            konaklamalar.append((kid, rid, *konaklama, random.choice(RESEPSIYON),
                                 random.choice(RESEPSIYON) if konaklama[2] is not None else None))
            kid += 1
        rid += 1
        onceki_bugun = (bit == 0)
        t = bit

# demo misafiri: birkaç geçmiş konaklama + bir gelecek rezervasyon
for r in [x for x in rezler if x[6] == "TAMAMLANDI"][:3] + \
         [x for x in rezler if x[6] == "ONAYLI" and x[3] > 5][:1]:
    rezler[rezler.index(r)] = (r[0], 1, *r[2:])

# ----------------------------------------------------------------- SQL
L = ["""-- =====================================================================
--  05 - ÖRNEK VERİ  (generate_seed.py tarafından üretildi — elle düzenlemeyin)
--  Tarihler @bugun'e göre görelidir.
-- =====================================================================
SET NAMES utf8mb4;
USE otel_db;
SET @bugun = CURDATE();
"""]

L.append("-- Oda tipleri")
L.append(bloklar("OdaTipi", ["OdaTipiID", "TipAdi", "Kapasite", "Aciklama", "TemelFiyat"],
                 [[q(a) for a in t] for t in ODA_TIPLERI]))
L.append("\n-- Odalar")
L.append(bloklar("Oda", ["OdaID", "OdaNo", "Kat", "OdaTipiID", "Durum"],
                 [[q(o[0]), q(o[1]), q(o[2]), q(o[3]), q("BOS")] for o in ODALAR]))

L.append("\n-- Sezon fiyatları")
sezon = []
for yil in YILLAR:
    for (ad, b, e, carpan) in SEZONLAR:
        for (tid, _, _, _, temel) in ODA_TIPLERI:
            sezon.append([q(tid), q(f"{ad} {yil}"), q(f"{yil}-{b}"), q(f"{yil}-{e}"),
                          q(round(temel * carpan / 10) * 10)])
L.append(bloklar("SezonFiyati", ["OdaTipiID", "SezonAdi", "BaslangicTarihi", "BitisTarihi", "GecelikFiyat"], sezon))

L.append("\n-- Ek hizmetler")
L.append(bloklar("EkHizmet", ["EkHizmetID", "HizmetAdi", "BirimFiyat"], [[q(a) for a in e] for e in EK_HIZMETLER]))
L.append("\n-- Personel")
L.append(bloklar("Personel", ["PersonelID", "Ad", "Soyad", "Gorev", "IseGirisTarihi", "Telefon"],
                 [[q(a) for a in p] for p in PERSONEL]))
L.append(f"\n-- Misafirler ({len(misafirler)})")
L.append(bloklar("Misafir", ["MisafirID", "Ad", "Soyad", "KimlikNo", "Telefon", "Eposta", "DogumTarihi", "KayitTarihi"],
                 [[q(m[0]), q(m[1]), q(m[2]), q(m[3]), q(m[4]), q(m[5]), q(m[6]), gun(-320 + m[0], "10:00:00")]
                  for m in misafirler]))

L.append("\n-- Kullanıcılar (şifreler BCrypt ile özetlenmiş)")
kullanicilar = [
    [q("yonetici"), q(HASH_YONETICI), q("YONETICI"), "NULL", q(1)],
    [q("resepsiyon"), q(HASH_RESEPSIYON), q("RESEPSIYON"), "NULL", q(2)],
    [q("resepsiyon2"), q(HASH_RESEPSIYON), q("RESEPSIYON"), "NULL", q(3)],
] + [[q(m[5]), q(HASH_MISAFIR), q("MISAFIR"), q(m[0]), "NULL"] for m in misafirler[:15]]
L.append(bloklar("Kullanici", ["KullaniciAdi", "SifreHash", "Rol", "MisafirID", "PersonelID"], kullanicilar))

L.append(f"\n-- Rezervasyonlar ({len(rezler)}) — çakışma tetikleyicisi her satırı denetler")
L.append(bloklar("Rezervasyon",
                 ["RezervasyonID", "MisafirID", "OdaID", "GirisTarihi", "CikisTarihi", "KisiSayisi", "Durum", "OlusturmaTarihi"],
                 [[q(r[0]), q(r[1]), q(r[2]), gun(r[3]), gun(r[4]), q(r[5]), q(r[6]), gun(r[7], r[8])] for r in rezler]))

L.append(f"\n-- Konaklamalar ({len(konaklamalar)})")
L.append(bloklar("Konaklama", ["KonaklamaID", "RezervasyonID", "GercekGiris", "GercekCikis", "GirisPersonelID", "CikisPersonelID"],
                 [[q(k), q(r), gun(gg, gs) if gs else "(NOW() - INTERVAL 2 HOUR)",
                   gun(cg, cs) if cg is not None else "NULL", q(gp), q(cp)]
                  for (k, r, gg, gs, cg, cs, gp, cp) in konaklamalar]))

L.append("\n-- Konaklama ek hizmetleri")
rez_by_id = {r[0]: r for r in rezler}
kh = []
for (k, r_id, gg, gs, cg, cs, gp, cp) in konaklamalar:
    r = rez_by_id[r_id]
    son = cg if cg is not None else 0
    for _ in range(random.choices([0, 1, 2, 3, 4], weights=[20, 30, 25, 15, 10])[0]):
        hz = random.choice(EK_HIZMETLER)
        g = min(random.randint(gg, max(gg, son - 1)) if son > gg else gg, 0)
        adet = random.randint(1, max(1, r[5])) if hz[0] in (1, 2) else random.randint(1, 2)
        kh.append([q(k), q(hz[0]), q(adet), q(hz[2]),
                   gun(g, f"{random.randint(19, 22):02d}:{random.randint(0, 59):02d}:00") if g < 0
                   else "(NOW() - INTERVAL 1 HOUR)"])
L.append(bloklar("KonaklamaHizmeti", ["KonaklamaID", "EkHizmetID", "Adet", "BirimFiyat", "Tarih"], kh))

L.append("""
-- Ödemeler: tamamlanan konaklamalarda toplam tutar tahsil edilmiştir
INSERT INTO Odeme (KonaklamaID, Tutar, OdemeTuru, OdemeTarihi, PersonelID)
SELECT k.KonaklamaID, fn_KonaklamaToplam(k.KonaklamaID),
       IF(MOD(k.KonaklamaID, 5) < 3, 'KART', 'NAKIT'), k.GercekCikis, k.CikisPersonelID
  FROM Konaklama k
 WHERE k.GercekCikis IS NOT NULL;

-- Otelde kalanların bir kısmından girişte kapora (ilk gece) alınmıştır
INSERT INTO Odeme (KonaklamaID, Tutar, OdemeTuru, OdemeTarihi, PersonelID)
SELECT k.KonaklamaID, fn_GecelikFiyat(o.OdaTipiID, r.GirisTarihi), 'KART', k.GercekGiris, k.GirisPersonelID
  FROM Konaklama k
  JOIN Rezervasyon r ON r.RezervasyonID = k.RezervasyonID
  JOIN Oda o ON o.OdaID = r.OdaID
 WHERE k.GercekCikis IS NULL AND MOD(k.KonaklamaID, 2) = 0;
""")

L.append("-- Temizlik kayıtları: her check-out'tan sonra")
L.append(bloklar("TemizlikKaydi", ["OdaID", "PersonelID", "Tarih", "Durum", "Notlar"],
                 [[q(rez_by_id[r_id][2]), q(random.choice(KAT_GOREVLISI)),
                   gun(cg, f"{random.randint(12, 15):02d}:{random.randint(0, 59):02d}:00"), q("TAMAMLANDI"), "NULL"]
                  for (k, r_id, gg, gs, cg, cs, gp, cp) in konaklamalar if cg is not None]))

L.append("""
-- Demo için: bugün girişi olmayan boş odalardan 2'si temizlikte, 1'i bakımda
UPDATE Oda o SET o.Durum = 'TEMIZLIKTE'
 WHERE o.Durum = 'BOS'
   AND o.OdaID NOT IN (SELECT OdaID FROM Rezervasyon
                        WHERE GirisTarihi <= @bugun + INTERVAL 1 DAY AND Durum IN ('BEKLEMEDE','ONAYLI'))
 ORDER BY o.OdaID LIMIT 2;

UPDATE Oda o SET o.Durum = 'BAKIMDA'
 WHERE o.Durum = 'BOS'
   AND o.OdaID NOT IN (SELECT OdaID FROM Rezervasyon
                        WHERE GirisTarihi <= @bugun + INTERVAL 1 DAY AND CikisTarihi > @bugun
                          AND Durum IN ('BEKLEMEDE','ONAYLI'))
 ORDER BY o.OdaID DESC LIMIT 1;

-- Kurulumda tetikleyicilerin yazdığı toplu log kayıtları temizlenir;
-- IslemLog yalnızca uygulamadaki gerçek işlemleri göstersin.
TRUNCATE TABLE IslemLog;
""")

OUT.write_text("\n".join(L), encoding="utf-8")
print(f"{OUT.name}: {len(rezler)} rezervasyon, {len(konaklamalar)} konaklama, "
      f"{len(misafirler)} misafir, {len(ODALAR)} oda, {len(kh)} ek hizmet, "
      f"bugün check-in bekleyen: {bugun_bekleyen}")