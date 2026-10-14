import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, tokenAl, tokenKaydet } from '../lib/api'
import type { GirisCevabi, Kullanici } from '../lib/tipler'

export type KayitBilgisi = {
  ad: string
  soyad: string
  kimlikNo: string
  eposta: string
  telefon?: string
  sifre: string
}

type OturumDegeri = {
  kullanici: Kullanici | null
  yukleniyor: boolean
  giris: (kullaniciAdi: string, sifre: string) => Promise<Kullanici>
  kayit: (bilgi: KayitBilgisi) => Promise<Kullanici>
  cikis: () => void
}

const OturumContext = createContext<OturumDegeri | null>(null)

/** Uygulamanın tamamını sarar; giriş yapan kullanıcıyı her yerden erişilebilir kılar. */
export function OturumSaglayici({ children }: { children: ReactNode }) {
  const [kullanici, setKullanici] = useState<Kullanici | null>(null)
  const [yukleniyor, setYukleniyor] = useState(true)

  useEffect(() => {
    // Token süresi dolunca (api.ts haber verir) oturumu kapat
    const dustu = () => setKullanici(null)
    window.addEventListener('oturum-dustu', dustu)

    // Sayfa yenilendiğinde: kayıtlı token varsa kim olduğumuzu backend'e sor
    if (tokenAl()) {
      api.get<Kullanici>('/api/auth/ben')
        .then(setKullanici)
        .catch(() => tokenKaydet(null))
        .finally(() => setYukleniyor(false))
    } else {
      setYukleniyor(false)
    }
    return () => window.removeEventListener('oturum-dustu', dustu)
  }, [])

  async function oturumAc(cevap: GirisCevabi) {
    tokenKaydet(cevap.token)
    setKullanici(cevap.kullanici)
    return cevap.kullanici
  }

  const deger: OturumDegeri = {
    kullanici,
    yukleniyor,
    giris: async (kullaniciAdi, sifre) =>
      oturumAc(await api.post<GirisCevabi>('/api/auth/giris', { kullaniciAdi, sifre })),
    kayit: async (bilgi) =>
      oturumAc(await api.post<GirisCevabi>('/api/auth/kayit', bilgi)),
    cikis: () => {
      tokenKaydet(null)
      setKullanici(null)
    },
  }

  return <OturumContext.Provider value={deger}>{children}</OturumContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useOturum() {
  const deger = useContext(OturumContext)
  if (!deger) throw new Error('useOturum, OturumSaglayici içinde kullanılmalı.')
  return deger
}