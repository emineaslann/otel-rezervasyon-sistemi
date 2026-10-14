import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { CalendarDays, Users, Search, Minus, Plus } from 'lucide-react'
import { isoTarih, geceSayisi } from '../lib/bicim'
import type { FormEvent } from 'react'

const ODA_KAPASITESI = 4

type Props = { baslangic?: { giris: string; cikis: string; kisi: number } }

export default function AramaKarti({ baslangic }: Props) {
  const navigate = useNavigate()
  const [giris, setGiris] = useState(baslangic?.giris ?? isoTarih(7))
  const [cikis, setCikis] = useState(baslangic?.cikis ?? isoTarih(10))
  const [yetiskin, setYetiskin] = useState(Math.min(baslangic?.kisi ?? 2, ODA_KAPASITESI))
  const [cocuk, setCocuk] = useState(0)
  const [misafirAcik, setMisafirAcik] = useState(false)
  const [hata, setHata] = useState<string | null>(null)
  const misafirRef = useRef<HTMLDivElement>(null)

  // Misafir seçicinin dışına tıklanınca kapansın
  useEffect(() => {
    const kapat = (e: MouseEvent) => {
      if (misafirRef.current && !misafirRef.current.contains(e.target as Node)) setMisafirAcik(false)
    }
    document.addEventListener('mousedown', kapat)
    return () => document.removeEventListener('mousedown', kapat)
  }, [])

  const toplam = yetiskin + cocuk
  const gece = geceSayisi(giris, cikis)

    function ara(e: FormEvent) {
    e.preventDefault()
    if (gece < 1) return setHata('Çıkış tarihi giriş tarihinden sonra olmalı.')
    if (gece > 30) return setHata('Tek rezervasyonda en fazla 30 gece seçilebilir.')
    setHata(null)
    navigate(`/arama?giris=${giris}&cikis=${cikis}&kisi=${toplam}`)
  }

  return (
    <form onSubmit={ara} className="rounded-2xl bg-white p-3 shadow-2xl shadow-deniz-950/20 ring-1 ring-slate-200">
      <div className="grid gap-2 md:grid-cols-[1fr_1fr_1fr_auto]">
        <label className="flex items-center gap-3 rounded-xl border border-slate-200 px-4 py-2.5 hover:border-deniz-500">
          <CalendarDays className="shrink-0 text-deniz-600" size={20} />
          <span className="flex-1">
            <span className="block text-xs text-slate-500">Giriş</span>
            <input type="date" value={giris} min={isoTarih(0)}
                   onChange={(e) => setGiris(e.target.value)}
                   className="w-full bg-transparent font-semibold outline-none" />
          </span>
        </label>

        <label className="flex items-center gap-3 rounded-xl border border-slate-200 px-4 py-2.5 hover:border-deniz-500">
          <CalendarDays className="shrink-0 text-deniz-600" size={20} />
          <span className="flex-1">
            <span className="block text-xs text-slate-500">Çıkış</span>
            <input type="date" value={cikis} min={giris}
                   onChange={(e) => setCikis(e.target.value)}
                   className="w-full bg-transparent font-semibold outline-none" />
          </span>
        </label>

        <div ref={misafirRef} className="relative">
          <button type="button" onClick={() => setMisafirAcik(!misafirAcik)}
                  className="flex h-full w-full items-center gap-3 rounded-xl border border-slate-200 px-4 py-2.5 text-left hover:border-deniz-500">
            <Users className="shrink-0 text-deniz-600" size={20} />
            <span>
              <span className="block text-xs text-slate-500">Misafirler</span>
              <span className="font-semibold">
                {yetiskin} yetişkin{cocuk > 0 && `, ${cocuk} çocuk`}
              </span>
            </span>
          </button>

          {misafirAcik && (
            <div className="absolute left-0 right-0 top-full z-20 mt-2 rounded-xl bg-white p-4 shadow-xl ring-1 ring-slate-200">
              <Sayac etiket="Yetişkin" aciklama="18 yaş ve üzeri" deger={yetiskin}
                     min={1} max={ODA_KAPASITESI - cocuk} degistir={setYetiskin} />
              <Sayac etiket="Çocuk" aciklama="0–17 yaş" deger={cocuk}
                     min={0} max={ODA_KAPASITESI - yetiskin} degistir={setCocuk} />
              <p className="mt-3 text-xs text-slate-500">Bir odada en fazla {ODA_KAPASITESI} kişi konaklayabilir.</p>
            </div>
          )}
        </div>

        <button type="submit"
                className="flex items-center justify-center gap-2 rounded-xl bg-deniz-700 px-8 py-3.5 font-semibold text-white hover:bg-deniz-800">
          <Search size={18} /> Ara
        </button>
      </div>

      {hata && <p className="px-2 pt-3 text-sm text-red-600">{hata}</p>}
    </form>
  )
}

function Sayac({ etiket, aciklama, deger, min, max, degistir }: {
  etiket: string; aciklama: string; deger: number; min: number; max: number
  degistir: (yeni: number) => void
}) {
  return (
    <div className="flex items-center justify-between py-2">
      <div>
        <p className="font-medium">{etiket}</p>
        <p className="text-xs text-slate-500">{aciklama}</p>
      </div>
      <div className="flex items-center gap-3">
        <button type="button" disabled={deger <= min} onClick={() => degistir(deger - 1)}
                className="grid h-8 w-8 place-items-center rounded-full border border-slate-300 disabled:opacity-30">
          <Minus size={14} />
        </button>
        <span className="w-4 text-center font-semibold">{deger}</span>
        <button type="button" disabled={deger >= max} onClick={() => degistir(deger + 1)}
                className="grid h-8 w-8 place-items-center rounded-full border border-slate-300 disabled:opacity-30">
          <Plus size={14} />
        </button>
      </div>
    </div>
  )
}