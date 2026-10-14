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