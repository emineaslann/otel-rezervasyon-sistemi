import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { BedDouble, Check, Users, Flame, SearchX, AlertCircle } from 'lucide-react'
import AramaKarti from '../components/AramaKarti'
import { apiGet, ApiHatasi } from '../lib/api'
import { para, kisaTarih, odaGorseli } from '../lib/bicim'
import type { BosOda } from '../lib/tipler'
import { useOturum } from '../context/OturumContext'

/** Aynı tipteki boş odalar tek kartta toplanır */
type TipSonucu = { ornek: BosOda; adet: number }
type Siralama = 'fiyat-artan' | 'fiyat-azalan' | 'kapasite'

export default function AramaSonuclari() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { kullanici } = useOturum()
  const giris = params.get('giris') ?? ''
  const cikis = params.get('cikis') ?? ''
  const kisi = Number(params.get('kisi') ?? 2)

  const [odalar, setOdalar] = useState<BosOda[] | null>(null)   // null = yükleniyor
  const [hata, setHata] = useState<string | null>(null)
  const [secilenTipler, setSecilenTipler] = useState<string[]>([])
  const [siralama, setSiralama] = useState<Siralama>('fiyat-artan')

  // Arama değişince prosedürü yeniden çağır
  useEffect(() => {
    setOdalar(null)
    setHata(null)
    apiGet<BosOda[]>(`/api/odalar/bos?giris=${giris}&cikis=${cikis}&kisi=${kisi}`)
      .then(setOdalar)
      .catch((e) => setHata(e instanceof ApiHatasi ? e.message : 'Arama yapılamadı.'))
  }, [giris, cikis, kisi])

  // Odaları tipe göre grupla
  const tumTipler = useMemo<TipSonucu[]>(() => {
    const harita = new Map<number, TipSonucu>()
    for (const oda of odalar ?? []) {
      const mevcut = harita.get(oda.odaTipiId)
      if (mevcut) mevcut.adet++
      else harita.set(oda.odaTipiId, { ornek: oda, adet: 1 })
    }
    return [...harita.values()]
  }, [odalar])

  // Filtre ve sıralama uygula
  const gosterilen = useMemo(() => {
    const liste = secilenTipler.length
      ? tumTipler.filter((t) => secilenTipler.includes(t.ornek.tipAdi))
      : [...tumTipler]
    return liste.sort((a, b) =>
      siralama === 'fiyat-artan' ? a.ornek.toplamFiyat - b.ornek.toplamFiyat
      : siralama === 'fiyat-azalan' ? b.ornek.toplamFiyat - a.ornek.toplamFiyat
      : b.ornek.kapasite - a.ornek.kapasite)
  }, [tumTipler, secilenTipler, siralama])

  function tipSec(tipAdi: string) {
    setSecilenTipler((onceki) =>
      onceki.includes(tipAdi) ? onceki.filter((t) => t !== tipAdi) : [...onceki, tipAdi])
  }

    function sec(t: TipSonucu) {
    const hedef = `/rezervasyon?odaId=${t.ornek.odaId}&giris=${giris}&cikis=${cikis}&kisi=${kisi}`
    // Giriş yapılmamışsa önce giriş sayfasına, sonra kaldığı yere dönsün
    navigate(kullanici ? hedef : `/giris?donus=${encodeURIComponent(hedef)}`)
  }

  const gece = odalar?.[0]?.geceSayisi

  return (
    <div className="min-h-screen bg-slate-50 pb-16">
      {/* ---------- Arama çubuğu ---------- */}
      <div className="border-b border-slate-200 bg-white px-6 py-5">
        <div className="mx-auto max-w-6xl">
          <AramaKarti baslangic={{ giris, cikis, kisi }} />
        </div>
      </div>

      <div className="mx-auto max-w-6xl px-6 pt-8">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-2">
          <div>
            <h1 className="font-serif text-3xl text-deniz-950">Müsait odalar</h1>
            <p className="mt-1 text-slate-600">
              {giris && kisaTarih(giris)} – {cikis && kisaTarih(cikis)}
              {gece !== undefined && ` · ${gece} gece`} · {kisi} misafir
            </p>
          </div>
          {odalar && <p className="text-sm text-slate-500">{gosterilen.length} oda tipi uygun</p>}
        </div>

        <div className="grid gap-6 lg:grid-cols-[250px_1fr]">
          {/* ---------- Filtreler ---------- */}
          <aside className="h-fit space-y-6 rounded-2xl bg-white p-5 ring-1 ring-slate-200 lg:sticky lg:top-24">
            <div>
              <p className="mb-3 font-semibold text-slate-800">Sırala</p>
              <select value={siralama} onChange={(e) => setSiralama(e.target.value as Siralama)}
                      className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm">
                <option value="fiyat-artan">Fiyat: düşükten yükseğe</option>
                <option value="fiyat-azalan">Fiyat: yüksekten düşüğe</option>
                <option value="kapasite">Kapasite: en geniş</option>
              </select>
            </div>
            <div>
              <p className="mb-3 font-semibold text-slate-800">Oda tipi</p>
              <div className="space-y-2">
                {tumTipler.map((t) => (
                  <label key={t.ornek.odaTipiId} className="flex cursor-pointer items-center justify-between text-sm">
                    <span className="flex items-center gap-2">
                      <input type="checkbox" checked={secilenTipler.includes(t.ornek.tipAdi)}
                             onChange={() => tipSec(t.ornek.tipAdi)} className="h-4 w-4 accent-deniz-700" />
                      {t.ornek.tipAdi}
                    </span>
                    <span className="text-slate-400">{t.adet}</span>
                  </label>
                ))}
              </div>
            </div>
          </aside>

          {/* ---------- Sonuçlar ---------- */}
          <section className="space-y-4">
            {hata && (
              <div className="flex items-center gap-3 rounded-2xl bg-red-50 p-5 text-red-700 ring-1 ring-red-200">
                <AlertCircle size={20} /> {hata}
              </div>
            )}

            {!hata && odalar === null && [1, 2, 3].map((i) => (
              <div key={i} className="h-56 animate-pulse rounded-2xl bg-white ring-1 ring-slate-200" />
            ))}

            {odalar && gosterilen.length === 0 && (
              <div className="rounded-2xl bg-white p-12 text-center ring-1 ring-slate-200">
                <SearchX className="mx-auto text-slate-400" size={40} />
                <p className="mt-4 font-serif text-2xl text-deniz-950">Bu tarihlerde uygun oda bulamadık</p>
                <p className="mt-2 text-slate-600">Tarihleri bir iki gün kaydırmayı ya da misafir sayısını değiştirmeyi deneyin.</p>
              </div>
            )}

            {gosterilen.map((t) => (
              <article key={t.ornek.odaTipiId}
                       className="grid overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 transition hover:shadow-lg md:grid-cols-[260px_1fr_210px]">
                <img src={odaGorseli(t.ornek.tipAdi)} alt={t.ornek.tipAdi}
                     className="h-52 w-full object-cover md:h-full" />

                <div className="p-5">
                  <h2 className="font-serif text-2xl text-deniz-950">{t.ornek.tipAdi} Oda</h2>
                  <p className="mt-2 text-sm text-slate-600">{t.ornek.aciklama}</p>
                  <div className="mt-4 flex flex-wrap gap-4 text-sm text-slate-600">
                    <span className="flex items-center gap-1.5"><Users size={16} /> {t.ornek.kapasite} kişiye kadar</span>
                    <span className="flex items-center gap-1.5"><BedDouble size={16} /> {t.ornek.tipAdi === 'Aile' ? '2 yatak odası' : '1 yatak odası'}</span>
                  </div>
                  <div className="mt-4 space-y-1.5 text-sm">
                    <p className="flex items-center gap-1.5 font-medium text-emerald-700"><Check size={16} /> Ücretsiz iptal</p>
                    <p className="flex items-center gap-1.5 text-emerald-700"><Check size={16} /> Ödeme konaklama sonunda</p>
                  </div>
                  {t.adet <= 2 && (
                    <p className="mt-4 inline-flex items-center gap-1.5 rounded-full bg-red-50 px-3 py-1 text-sm font-semibold text-red-600">
                      <Flame size={15} /> Bu tipte son {t.adet} oda!
                    </p>
                  )}
                </div>

                <div className="flex flex-col justify-between gap-4 border-t border-slate-100 p-5 md:border-l md:border-t-0 md:text-right">
                  <div>
                    <p className="text-sm text-slate-500">Gecelik ort. {para(t.ornek.ortalamaGecelik)}</p>
                    <p className="mt-1 text-3xl font-bold text-deniz-900">{para(t.ornek.toplamFiyat)}</p>
                    <p className="text-xs text-slate-500">{t.ornek.geceSayisi} gece için toplam</p>
                  </div>
                  <button onClick={() => sec(t)}
                          className="rounded-xl bg-deniz-700 px-5 py-3 font-semibold text-white hover:bg-deniz-800">
                    Seç
                  </button>
                </div>
              </article>
            ))}
          </section>
        </div>
      </div>
    </div>
  )
}