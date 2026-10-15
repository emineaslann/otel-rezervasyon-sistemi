import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { CalendarDays, Users, Moon, Loader2, Luggage, X, AlertCircle } from 'lucide-react'
import { useOturum } from '../context/OturumContext'
import { api, ApiHatasi } from '../lib/api'
import { para, kisaTarih, odaGorseli } from '../lib/bicim'
import type { MisafirRezervasyonu } from '../lib/tipler'

type Sekme = 'yaklasan' | 'gecmis' | 'iptal'

export default function Hesabim() {
  const { kullanici } = useOturum()
  const [liste, setListe] = useState<MisafirRezervasyonu[] | null>(null)
  const [sekme, setSekme] = useState<Sekme>('yaklasan')
  const [iptalEdilecek, setIptalEdilecek] = useState<MisafirRezervasyonu | null>(null)
  const [bildirim, setBildirim] = useState<string | null>(null)

  const yukle = () =>
    api.get<MisafirRezervasyonu[]>('/api/rezervasyonlar/benim').then(setListe).catch(() => setListe([]))

  useEffect(() => { yukle() }, [])

  // View'dan gelen tüm rezervasyonları sekmelere ayır
  const gruplar = useMemo(() => {
    const l = liste ?? []
    return {
      yaklasan: l.filter((r) => r.durum === 'BEKLEMEDE' || r.durum === 'ONAYLI')
                 .sort((a, b) => a.girisTarihi.localeCompare(b.girisTarihi)),
      gecmis: l.filter((r) => r.durum === 'TAMAMLANDI')
               .sort((a, b) => b.girisTarihi.localeCompare(a.girisTarihi)),
      iptal: l.filter((r) => r.durum === 'IPTAL')
              .sort((a, b) => b.girisTarihi.localeCompare(a.girisTarihi)),
    }
  }, [liste])

  const toplamGece = gruplar.gecmis.reduce((t, r) => t + r.geceSayisi, 0)

  return (
    <div className="min-h-screen bg-slate-50 pb-16">
      {/* ---------- Karşılama bandı ---------- */}
      <div className="bg-deniz-900 text-white">
        <div className="mx-auto max-w-5xl px-6 py-10">
          <p className="text-sm text-deniz-200">Hesabım</p>
          <h1 className="mt-1 font-serif text-4xl">Merhaba, {kullanici?.adSoyad.split(' ')[0]}</h1>
          <div className="mt-6 flex flex-wrap gap-8 text-sm">
            <Ozet deger={gruplar.yaklasan.length} etiket="yaklaşan konaklama" />
            <Ozet deger={gruplar.gecmis.length} etiket="tamamlanan konaklama" />
            <Ozet deger={toplamGece} etiket="gece bizimle" />
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-5xl px-6">
        {/* ---------- Sekmeler ---------- */}
        <div className="-mt-6 flex gap-1 rounded-2xl bg-white p-1.5 shadow-sm ring-1 ring-slate-200">
          {([
            ['yaklasan', 'Yaklaşan', gruplar.yaklasan.length],
            ['gecmis', 'Geçmiş', gruplar.gecmis.length],
            ['iptal', 'İptal edilenler', gruplar.iptal.length],
          ] as const).map(([anahtar, ad, sayi]) => (
            <button key={anahtar} onClick={() => setSekme(anahtar)}
                    className={`flex-1 rounded-xl py-3 text-sm font-semibold transition ${
                      sekme === anahtar ? 'bg-deniz-700 text-white' : 'text-slate-600 hover:bg-slate-50'}`}>
              {ad} <span className="opacity-70">({sayi})</span>
            </button>
          ))}
        </div>

        {bildirim && (
          <p className="mt-6 rounded-xl bg-emerald-50 px-5 py-3 text-sm font-medium text-emerald-800 ring-1 ring-emerald-200">
            {bildirim}
          </p>
        )}

        {/* ---------- Liste ---------- */}
        <div className="mt-6 space-y-4">
          {liste === null && [1, 2].map((i) => (
            <div key={i} className="h-40 animate-pulse rounded-2xl bg-white ring-1 ring-slate-200" />
          ))}

          {liste && gruplar[sekme].length === 0 && (
            <div className="rounded-2xl bg-white p-12 text-center ring-1 ring-slate-200">
              <Luggage className="mx-auto text-slate-300" size={44} />
              <p className="mt-4 font-serif text-2xl text-deniz-950">
                {sekme === 'yaklasan' ? 'Planlanmış bir konaklamanız yok' : 'Burada henüz bir şey yok'}
              </p>
              {sekme === 'yaklasan' && (
                <Link to="/" className="mt-6 inline-block rounded-xl bg-deniz-700 px-6 py-3 font-semibold text-white hover:bg-deniz-800">
                  Oda ara
                </Link>
              )}
            </div>
          )}

          {gruplar[sekme].map((r) => (
            <RezervasyonKarti key={r.rezervasyonId} r={r} iptalEt={() => setIptalEdilecek(r)} />
          ))}
        </div>
      </div>

      {iptalEdilecek && (
        <IptalOnayi
          r={iptalEdilecek}
          kapat={() => setIptalEdilecek(null)}
          tamam={() => {
            setBildirim(`#${iptalEdilecek.rezervasyonId} numaralı rezervasyonunuz iptal edildi.`)
            setIptalEdilecek(null)
            yukle()
          }}
        />
      )}
    </div>
  )
}

// ---------------------------------------------------------- kart

function RezervasyonKarti({ r, iptalEt }: { r: MisafirRezervasyonu; iptalEt: () => void }) {
  const konakliyor = r.konaklamaId !== null && r.gercekCikis === null
  const iptalEdilebilir = (r.durum === 'BEKLEMEDE' || r.durum === 'ONAYLI') && r.konaklamaId === null

  return (
    <article className="grid overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200 sm:grid-cols-[200px_1fr]">
      <img src={odaGorseli(r.tipAdi)} alt={r.tipAdi} className="h-40 w-full object-cover sm:h-full" />
      <div className="flex flex-col justify-between gap-4 p-5 sm:flex-row">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="font-serif text-2xl text-deniz-950">{r.tipAdi} Oda</h2>
            <DurumRozeti durum={r.durum} konakliyor={konakliyor} />
          </div>
          <p className="mt-1 text-sm text-slate-500">Rezervasyon #{r.rezervasyonId} · Oda {r.odaNo}</p>
          <div className="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-sm text-slate-600">
            <span className="flex items-center gap-1.5"><CalendarDays size={15} /> {kisaTarih(r.girisTarihi)} – {kisaTarih(r.cikisTarihi)}</span>
            <span className="flex items-center gap-1.5"><Moon size={15} /> {r.geceSayisi} gece</span>
            <span className="flex items-center gap-1.5"><Users size={15} /> {r.kisiSayisi} kişi</span>
          </div>
        </div>
        <div className="flex flex-col items-start justify-between gap-3 sm:items-end">
          <div className="sm:text-right">
            <p className="text-xl font-bold text-deniz-900">{para(r.tutar)}</p>
            <p className="text-xs text-slate-500">
              {r.durum === 'TAMAMLANDI' ? `Ödendi: ${para(r.odenen)}` : 'Ödeme konaklama sonunda'}
            </p>
          </div>
          {iptalEdilebilir && (
            <button onClick={iptalEt}
                    className="rounded-lg px-4 py-2 text-sm font-semibold text-red-600 ring-1 ring-red-200 hover:bg-red-50">
              İptal et
            </button>
          )}
        </div>
      </div>
    </article>
  )
}

function DurumRozeti({ durum, konakliyor }: { durum: MisafirRezervasyonu['durum']; konakliyor: boolean }) {
  const [metin, stil] = konakliyor
    ? ['Şu an oteldesiniz', 'bg-sky-100 text-sky-800']
    : {
        BEKLEMEDE: ['Onay bekliyor', 'bg-amber-100 text-amber-800'],
        ONAYLI: ['Onaylandı', 'bg-emerald-100 text-emerald-800'],
        TAMAMLANDI: ['Tamamlandı', 'bg-slate-100 text-slate-700'],
        IPTAL: ['İptal edildi', 'bg-red-100 text-red-700'],
      }[durum]
  return <span className={`rounded-full px-3 py-1 text-xs font-semibold ${stil}`}>{metin}</span>
}

// ---------------------------------------------------------- iptal onay penceresi

function IptalOnayi({ r, kapat, tamam }: { r: MisafirRezervasyonu; kapat: () => void; tamam: () => void }) {
  const [bekliyor, setBekliyor] = useState(false)
  const [hata, setHata] = useState<string | null>(null)

  async function onayla() {
    setBekliyor(true)
    setHata(null)
    try {
      await api.put(`/api/rezervasyonlar/${r.rezervasyonId}/iptal`)   // sp_RezervasyonIptal
      tamam()
    } catch (e) {
      setHata(e instanceof ApiHatasi ? e.message : 'İptal edilemedi.')
      setBekliyor(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-deniz-950/50 p-6 backdrop-blur-sm" onClick={kapat}>
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start justify-between">
          <h2 className="font-serif text-2xl text-deniz-950">Rezervasyonu iptal et</h2>
          <button onClick={kapat} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
        </div>
        <p className="mt-3 text-slate-600">
          <b>{r.tipAdi} Oda</b>, {kisaTarih(r.girisTarihi)} – {kisaTarih(r.cikisTarihi)} tarihli
          #{r.rezervasyonId} numaralı rezervasyonunuz iptal edilecek. Bu işlem geri alınamaz.
        </p>
        {hata && <p className="mt-4 flex items-center gap-2 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700"><AlertCircle size={16} /> {hata}</p>}
        <div className="mt-6 flex justify-end gap-3">
          <button onClick={kapat} className="rounded-xl px-5 py-2.5 font-semibold text-slate-700 hover:bg-slate-100">Vazgeç</button>
          <button onClick={onayla} disabled={bekliyor}
                  className="flex items-center gap-2 rounded-xl bg-red-600 px-5 py-2.5 font-semibold text-white hover:bg-red-700 disabled:opacity-60">
            {bekliyor && <Loader2 size={16} className="animate-spin" />} İptal et
          </button>
        </div>
      </div>
    </div>
  )
}

function Ozet({ deger, etiket }: { deger: number; etiket: string }) {
  return <p><span className="font-serif text-3xl text-pirinc-400">{deger}</span> <span className="text-deniz-200">{etiket}</span></p>
}