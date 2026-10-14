import { useEffect, useState } from 'react'
import { BedDouble, Users, Coffee, Sparkles, Waves, Wifi, Car, ConciergeBell } from 'lucide-react'
import AramaKarti from '../components/AramaKarti'
import { apiGet } from '../lib/api'
import { para, odaGorseli } from '../lib/bicim'
import type { OdaTipi } from '../lib/tipler'

export default function AnaSayfa() {
  const [tipler, setTipler] = useState<OdaTipi[]>([])

  useEffect(() => {
    apiGet<OdaTipi[]>('/api/oda-tipleri').then(setTipler).catch(() => setTipler([]))
  }, [])

  return (
    <>
      {/* ---------------- Giriş alanı ---------------- */}
      <section className="relative h-140 md:h-150">
        <img src="/img/hero.jpg" alt="Kıyı Otel" className="absolute inset-0 h-full w-full object-cover" />
        <div className="absolute inset-0 bg-linear-to-b from-deniz-950/70 via-deniz-950/30 to-deniz-950/70" />

        <div className="relative mx-auto max-w-6xl px-6 pt-24 text-white md:pt-32">
          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-kum-200">Deniz kenarında butik otel</p>
          <h1 className="mt-4 max-w-2xl font-serif text-5xl leading-tight md:text-6xl">
            Denize bakan odalarda, acelesiz günler.
          </h1>
          <p className="mt-5 max-w-xl text-lg text-white/85">
            Tarihlerinizi seçin; o geceler için boş odaları ve sezona göre hesaplanmış fiyatları anında görün.
          </p>
        </div>

        <div className="absolute inset-x-0 bottom-0 translate-y-1/2 px-6">
          <div className="mx-auto max-w-5xl"><AramaKarti /></div>
        </div>
      </section>

      <div className="h-40 md:h-24" />

      {/* ---------------- Odalar ---------------- */}
      <section id="odalar" className="mx-auto max-w-6xl scroll-mt-24 px-6 pt-10">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-pirinc-600">Konaklama</p>
        <h2 className="mt-2 font-serif text-4xl text-deniz-950">Odalarımız</h2>

        <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {tipler.map((t) => (
            <article key={t.id}
                     className="group overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 transition hover:-translate-y-1 hover:shadow-xl">
              <div className="aspect-4/3 overflow-hidden">
                <img src={odaGorseli(t.tipAdi)} alt={t.tipAdi}
                     className="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
              </div>
              <div className="p-5">
                <h3 className="font-serif text-2xl text-deniz-950">{t.tipAdi}</h3>
                <p className="mt-2 line-clamp-2 text-sm text-slate-600">{t.aciklama}</p>
                <div className="mt-4 flex items-center gap-4 text-sm text-slate-500">
                  <span className="flex items-center gap-1"><Users size={15} /> {t.kapasite} kişi</span>
                  <span className="flex items-center gap-1"><BedDouble size={15} /> {t.tipAdi === 'Aile' ? '2 yatak odası' : '1 yatak odası'}</span>
                </div>
                <p className="mt-4 border-t border-slate-100 pt-4 text-sm text-slate-500">
                  Gecelik <span className="text-xl font-bold text-deniz-800">{para(t.temelFiyat)}</span>'den başlayan
                </p>
              </div>
            </article>
          ))}
        </div>
      </section>

      {/* ---------------- Olanaklar ---------------- */}
      <section id="olanaklar" className="mx-auto mt-24 max-w-6xl scroll-mt-24 px-6">
        <div className="grid items-center gap-10 lg:grid-cols-2">
          <div className="grid grid-cols-2 gap-4">
            <img src="/img/spa.jpg" alt="Spa" className="aspect-3/4 w-full rounded-2xl object-cover" />
            <img src="/img/kahvalti.jpg" alt="Kahvaltı" className="mt-12 aspect-3/4 w-full rounded-2xl object-cover" />
          </div>
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-pirinc-600">Olanaklar</p>
            <h2 className="mt-2 font-serif text-4xl text-deniz-950">Konaklamanızı tamamlayan ayrıntılar</h2>
            <p className="mt-4 text-slate-600">
              Spa'dan havalimanı transferine kadar tüm ek hizmetler odanızın hesabına eklenir; çıkışta tek seferde ödersiniz.
            </p>
            <div className="mt-8 grid grid-cols-2 gap-5">
              {[
                { ikon: Coffee, ad: 'Açık büfe kahvaltı' },
                { ikon: Sparkles, ad: 'Spa ve masaj' },
                { ikon: Waves, ad: 'Denize sıfır' },
                { ikon: Wifi, ad: 'Ücretsiz Wi-Fi' },
                { ikon: Car, ad: 'Otopark' },
                { ikon: ConciergeBell, ad: '7/24 oda servisi' },
              ].map(({ ikon: Ikon, ad }) => (
                <div key={ad} className="flex items-center gap-3">
                  <span className="grid h-10 w-10 place-items-center rounded-full bg-deniz-50 text-deniz-700">
                    <Ikon size={18} />
                  </span>
                  <span className="font-medium text-slate-700">{ad}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>
    </>
  )
}