import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import {
  CalendarDays, Users, Check, Clock, ShieldCheck, Wallet, Loader2,
  AlertCircle, CircleCheckBig, ArrowLeft,
} from 'lucide-react'
import { useOturum } from '../context/OturumContext'
import { api, ApiHatasi } from '../lib/api'
import { para, kisaTarih, odaGorseli } from '../lib/bicim'
import type { BosOda, FiyatSatiri, RezervasyonSonucu } from '../lib/tipler'

export default function Rezervasyon() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { kullanici } = useOturum()

  const odaId = Number(params.get('odaId'))
  const giris = params.get('giris') ?? ''
  const cikis = params.get('cikis') ?? ''
  const kisi = Number(params.get('kisi') ?? 1)
  const aramaAdresi = `/arama?giris=${giris}&cikis=${cikis}&kisi=${kisi}`

  const [oda, setOda] = useState<BosOda | null>(null)
  const [dokum, setDokum] = useState<FiyatSatiri[]>([])
  const [yukleniyor, setYukleniyor] = useState(true)
  const [musaitDegil, setMusaitDegil] = useState(false)
  const [kosullar, setKosullar] = useState(false)
  const [gonderiliyor, setGonderiliyor] = useState(false)
  const [hata, setHata] = useState<string | null>(null)
  const [sonuc, setSonuc] = useState<RezervasyonSonucu | null>(null)

  // Oda hâlâ boş mu? (Arama ile rezervasyon arasında başkası almış olabilir)
  useEffect(() => {
    api.get<BosOda[]>(`/api/odalar/bos?giris=${giris}&cikis=${cikis}&kisi=${kisi}`)
      .then(async (liste) => {
        const bulunan = liste.find((o) => o.odaId === odaId)
        if (!bulunan) { setMusaitDegil(true); return }
        setOda(bulunan)
        setDokum(await api.get<FiyatSatiri[]>(
          `/api/fiyat-dokumu?odaTipiId=${bulunan.odaTipiId}&giris=${giris}&cikis=${cikis}`))
      })
      .catch(() => setMusaitDegil(true))
      .finally(() => setYukleniyor(false))
  }, [odaId, giris, cikis, kisi])

  async function tamamla() {
    setGonderiliyor(true)
    setHata(null)
    try {
      const r = await api.post<RezervasyonSonucu>('/api/rezervasyonlar', {
        odaId, girisTarihi: giris, cikisTarihi: cikis, kisiSayisi: kisi,
      })
      setSonuc(r)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (e) {
      // 409: trigger'ın "oda bu tarihlerde dolu" mesajı burada görünür
      setHata(e instanceof ApiHatasi ? e.message : 'Rezervasyon oluşturulamadı.')
    } finally {
      setGonderiliyor(false)
    }
  }

  // ---------------------------------------------------------- yükleniyor
  if (yukleniyor) {
    return <div className="grid min-h-[60vh] place-items-center"><Loader2 className="animate-spin text-deniz-600" size={36} /></div>
  }

  // ---------------------------------------------------------- oda kaçtı
  if (musaitDegil || !oda) {
    return (
      <div className="mx-auto max-w-lg px-6 py-24 text-center">
        <AlertCircle className="mx-auto text-amber-500" size={44} />
        <h1 className="mt-4 font-serif text-3xl text-deniz-950">Bu oda artık müsait değil</h1>
        <p className="mt-3 text-slate-600">Siz incelerken başka bir misafir bu odayı ayırtmış olabilir.</p>
        <Link to={aramaAdresi} className="mt-8 inline-block rounded-xl bg-deniz-700 px-6 py-3 font-semibold text-white hover:bg-deniz-800">
          Diğer odalara bak
        </Link>
      </div>
    )
  }

  // ---------------------------------------------------------- onay ekranı
  if (sonuc) {
    return (
      <div className="bg-slate-50 px-6 py-16">
        <div className="mx-auto max-w-2xl rounded-3xl bg-white p-10 text-center shadow-sm ring-1 ring-slate-200">
          <CircleCheckBig className="mx-auto text-emerald-600" size={56} />
          <h1 className="mt-5 font-serif text-4xl text-deniz-950">Rezervasyon talebiniz alındı</h1>
          <p className="mt-3 text-slate-600">
            Rezervasyon numaranız <span className="font-bold text-deniz-800">#{sonuc.rezervasyonId}</span>.
            Resepsiyon onayladığında e-posta ile bilgilendirileceksiniz.
          </p>
          <div className="mx-auto mt-8 grid max-w-md gap-3 rounded-2xl bg-kum-50 p-6 text-left text-sm">
            <Satir etiket="Oda" deger={`${sonuc.tipAdi} · Oda ${sonuc.odaNo}`} />
            <Satir etiket="Tarihler" deger={`${kisaTarih(sonuc.girisTarihi)} – ${kisaTarih(sonuc.cikisTarihi)}`} />
            <Satir etiket="Misafir" deger={`${sonuc.kisiSayisi} kişi · ${sonuc.geceSayisi} gece`} />
            <Satir etiket="Durum" deger="Onay bekliyor" />
            <div className="border-t border-kum-200 pt-3">
              <Satir etiket="Toplam" deger={para(sonuc.toplamFiyat)} kalin />
            </div>
          </div>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link to="/hesabim" className="rounded-xl bg-deniz-700 px-6 py-3 font-semibold text-white hover:bg-deniz-800">Rezervasyonlarım</Link>
            <Link to="/" className="rounded-xl px-6 py-3 font-semibold text-deniz-800 ring-1 ring-slate-300 hover:bg-slate-50">Ana sayfa</Link>
          </div>
        </div>
      </div>
    )
  }

  // ---------------------------------------------------------- form
  const toplam = dokum.reduce((t, g) => t + g.fiyat, 0)

  return (
    <div className="bg-slate-50 pb-16">
      <div className="mx-auto max-w-6xl px-6 pt-8">
        <button onClick={() => navigate(aramaAdresi)} className="flex items-center gap-1 text-sm font-medium text-deniz-700 hover:underline">
          <ArrowLeft size={16} /> Arama sonuçlarına dön
        </button>
        <h1 className="mt-4 font-serif text-4xl text-deniz-950">Rezervasyonunuzu tamamlayın</h1>

        <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_380px]">
          {/* ---------- Sol ---------- */}
          <div className="space-y-6">
            <Kart baslik="1. Misafir bilgileri">
              <div className="grid gap-4 sm:grid-cols-2">
                <SaltOkunur etiket="Ad soyad" deger={kullanici?.adSoyad ?? ''} />
                <SaltOkunur etiket="E-posta" deger={kullanici?.kullaniciAdi ?? ''} />
              </div>
              <p className="mt-3 text-sm text-slate-500">Onay e-postası bu adrese gönderilecek.</p>
            </Kart>

            <Kart baslik="2. Konaklama koşulları">
              <ul className="space-y-3 text-sm text-slate-700">
                <Kosul ikon={Clock}>Giriş 14:00'ten itibaren, çıkış 12:00'ye kadar</Kosul>
                <Kosul ikon={ShieldCheck}>Girişten önce <b>ücretsiz iptal</b></Kosul>
                <Kosul ikon={Wallet}>Ödeme konaklama sonunda otelde alınır; ek hizmetler hesabınıza eklenir</Kosul>
              </ul>
              <label className="mt-5 flex cursor-pointer items-start gap-3 rounded-xl bg-kum-50 p-4 text-sm">
                <input type="checkbox" checked={kosullar} onChange={(e) => setKosullar(e.target.checked)}
                       className="mt-0.5 h-4 w-4 accent-deniz-700" />
                Konaklama koşullarını okudum ve kabul ediyorum.
              </label>
            </Kart>

            {hata && (
              <div className="rounded-2xl bg-red-50 p-5 ring-1 ring-red-200">
                <p className="flex items-center gap-2 font-semibold text-red-700"><AlertCircle size={18} /> Rezervasyon yapılamadı</p>
                <p className="mt-1 text-sm text-red-700">{hata}</p>
                <Link to={aramaAdresi} className="mt-3 inline-block text-sm font-semibold text-deniz-700 hover:underline">Başka oda ara</Link>
              </div>
            )}

            <button onClick={tamamla} disabled={!kosullar || gonderiliyor}
                    className="flex w-full items-center justify-center gap-2 rounded-xl bg-deniz-700 py-4 text-lg font-semibold text-white hover:bg-deniz-800 disabled:cursor-not-allowed disabled:opacity-50">
              {gonderiliyor && <Loader2 className="animate-spin" size={20} />}
              Rezervasyonu tamamla
            </button>
          </div>

          {/* ---------- Sağ: özet ---------- */}
          <aside className="h-fit overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 lg:sticky lg:top-24">
            <img src={odaGorseli(oda.tipAdi)} alt={oda.tipAdi} className="h-44 w-full object-cover" />
            <div className="p-6">
              <h2 className="font-serif text-2xl text-deniz-950">{oda.tipAdi} Oda</h2>
              <div className="mt-3 space-y-1.5 text-sm text-slate-600">
                <p className="flex items-center gap-2"><CalendarDays size={16} /> {kisaTarih(giris)} – {kisaTarih(cikis)} · {dokum.length} gece</p>
                <p className="flex items-center gap-2"><Users size={16} /> {kisi} misafir</p>
                <p className="flex items-center gap-2 text-emerald-700"><Check size={16} /> Ücretsiz iptal</p>
              </div>

              <div className="mt-5 border-t border-slate-100 pt-4">
                <p className="mb-2 text-sm font-semibold text-slate-800">Fiyat dökümü</p>
                <div className="space-y-1.5 text-sm">
                  {dokum.map((g) => (
                    <div key={g.gece} className="flex justify-between">
                      <span className="text-slate-600">{kisaTarih(g.gece)} <span className="text-slate-400">· {g.sezon}</span></span>
                      <span>{para(g.fiyat)}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="mt-4 flex items-end justify-between border-t border-slate-200 pt-4">
                <span className="font-semibold text-slate-800">Toplam</span>
                <span className="text-3xl font-bold text-deniz-900">{para(toplam)}</span>
              </div>
              <p className="mt-1 text-right text-xs text-slate-500">Vergiler dahil · ödeme otelde</p>
            </div>
          </aside>
        </div>
      </div>
    </div>
  )
}

// ---------------------------------------------------------- küçük parçalar

function Kart({ baslik, children }: { baslik: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
      <h2 className="mb-4 text-lg font-semibold text-deniz-950">{baslik}</h2>
      {children}
    </section>
  )
}

function SaltOkunur({ etiket, deger }: { etiket: string; deger: string }) {
  return (
    <div>
      <p className="text-xs text-slate-500">{etiket}</p>
      <p className="mt-1 rounded-xl bg-slate-50 px-4 py-3 font-medium text-slate-800">{deger}</p>
    </div>
  )
}

function Kosul({ ikon: Ikon, children }: { ikon: typeof Clock; children: React.ReactNode }) {
  return <li className="flex items-start gap-3"><Ikon size={18} className="mt-0.5 shrink-0 text-deniz-600" /><span>{children}</span></li>
}

function Satir({ etiket, deger, kalin }: { etiket: string; deger: string; kalin?: boolean }) {
  return (
    <div className="flex justify-between">
      <span className="text-slate-500">{etiket}</span>
      <span className={kalin ? 'text-lg font-bold text-deniz-900' : 'font-medium text-slate-800'}>{deger}</span>
    </div>
  )
}