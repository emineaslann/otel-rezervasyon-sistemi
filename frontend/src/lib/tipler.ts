/** Backend'in döndürdüğü veri yapıları (Java'daki DTO'ların karşılığı) */

export type OdaTipi = {
  id: number
  tipAdi: string
  kapasite: number
  aciklama: string
  temelFiyat: number
}

/** sp_BosOdalariListele prosedürünün döndürdüğü her satır */
export type BosOda = {
  odaId: number
  odaNo: string
  kat: number
  odaTipiId: number
  tipAdi: string
  kapasite: number
  aciklama: string
  geceSayisi: number
  toplamFiyat: number
  ortalamaGecelik: number
}


export type Rol = 'MISAFIR' | 'RESEPSIYON' | 'YONETICI'

/** /api/auth/ben ve giriş cevabındaki kullanıcı */
export type Kullanici = {
  id: number
  kullaniciAdi: string
  rol: Rol
  adSoyad: string
  misafirId: number | null
  personelId: number | null
}

export type GirisCevabi = {
  token: string
  tip: string
  gecerlilikSaniye: number
  kullanici: Kullanici
}

/** /api/fiyat-dokumu satırı */
export type FiyatSatiri = {
  gece: string
  sezon: string
  fiyat: number
}

/** sp_RezervasyonOlustur'un döndürdüğü özet */
export type RezervasyonSonucu = {
  rezervasyonId: number
  durum: string
  girisTarihi: string
  cikisTarihi: string
  kisiSayisi: number
  odaNo: string
  tipAdi: string
  geceSayisi: number
  toplamFiyat: number
}

/** vw_MisafirRezervasyonlari satırı */
export type MisafirRezervasyonu = {
  rezervasyonId: number
  girisTarihi: string
  cikisTarihi: string
  kisiSayisi: number
  durum: 'BEKLEMEDE' | 'ONAYLI' | 'IPTAL' | 'TAMAMLANDI'
  olusturmaTarihi: string
  geceSayisi: number
  odaNo: string
  tipAdi: string
  konaklamaId: number | null
  gercekGiris: string | null
  gercekCikis: string | null
  tutar: number
  odenen: number
}