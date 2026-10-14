import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { Waves, Loader2, AlertCircle } from 'lucide-react'
import { useOturum } from '../context/OturumContext'
import { ApiHatasi } from '../lib/api'
import type { Kullanici } from '../lib/tipler'

type Sekme = 'giris' | 'kayit'

export default function Giris() {
  const { giris, kayit } = useOturum()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const donus = params.get('donus')     // giriş sonrası dönülecek sayfa

  const [sekme, setSekme] = useState<Sekme>('giris')
  const [bekliyor, setBekliyor] = useState(false)
  const [hata, setHata] = useState<string | null>(null)
  const [alanHatalari, setAlanHatalari] = useState<Record<string, string>>({})

  const [kullaniciAdi, setKullaniciAdi] = useState('')
  const [sifre, setSifre] = useState('')
  const [kayitBilgi, setKayitBilgi] = useState({ ad: '', soyad: '', kimlikNo: '', eposta: '', telefon: '', sifre: '' })

  /** Role göre doğru sayfaya yönlendir */
  function yonlendir(k: Kullanici) {
    if (k.rol === 'RESEPSIYON') navigate('/panel/resepsiyon')
    else if (k.rol === 'YONETICI') navigate('/panel/yonetim')
    else navigate(donus ?? '/hesabim')
  }

  async function gonder(e: FormEvent) {
    e.preventDefault()
    setBekliyor(true)
    setHata(null)
    setAlanHatalari({})
    try {
      const k = sekme === 'giris'
        ? await giris(kullaniciAdi.trim(), sifre)
        : await kayit({ ...kayitBilgi, telefon: kayitBilgi.telefon || undefined })
      yonlendir(k)
    } catch (err) {
      if (err instanceof ApiHatasi) {
        setHata(err.message)
        if (err.alanlar) setAlanHatalari(err.alanlar)
      } else setHata('Beklenmeyen bir hata oluştu.')
    } finally {
      setBekliyor(false)
    }
  }

  const kayitAlani = (ad: keyof typeof kayitBilgi, etiket: string, tip = 'text') => (
    <Alan etiket={etiket} hata={alanHatalari[ad]}>
      <input type={tip} value={kayitBilgi[ad]} required={ad !== 'telefon'}
             onChange={(e) => setKayitBilgi({ ...kayitBilgi, [ad]: e.target.value })}
             className={girdiStili} />
    </Alan>
  )

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* ---------- Sol: fotoğraf ---------- */}
      <div className="relative hidden lg:block">
        <img src="/img/spa.jpg" alt="" className="absolute inset-0 h-full w-full object-cover" />
        <div className="absolute inset-0 bg-linear-to-t from-deniz-950/90 via-deniz-950/30 to-transparent" />
        <div className="absolute bottom-0 p-12 text-white">
          <p className="font-serif text-4xl leading-tight">"Sabah denize karşı kahvaltı,<br />akşam spa. Tekrar geleceğiz."</p>
          <p className="mt-4 text-deniz-200">— Misafir yorumu</p>
        </div>
      </div>

      {/* ---------- Sağ: form ---------- */}
      <div className="flex flex-col px-6 py-10 sm:px-12">
        <Link to="/" className="flex items-center gap-2">
          <span className="grid h-9 w-9 place-items-center rounded-full bg-deniz-700 text-white"><Waves size={18} /></span>
          <span className="font-serif text-2xl font-semibold text-deniz-900">Kıyı Otel</span>
        </Link>

        <div className="mx-auto my-auto w-full max-w-md py-10">
          <h1 className="font-serif text-4xl text-deniz-950">
            {sekme === 'giris' ? 'Tekrar hoş geldiniz' : 'Hesap oluşturun'}
          </h1>
          <p className="mt-2 text-slate-600">
            {donus ? 'Rezervasyonunuzu tamamlamak için giriş yapın.' : 'Rezervasyonlarınızı tek yerden yönetin.'}
          </p>

          <div className="mt-8 grid grid-cols-2 rounded-xl bg-slate-100 p-1">
            {(['giris', 'kayit'] as Sekme[]).map((s) => (
              <button key={s} type="button" onClick={() => { setSekme(s); setHata(null); setAlanHatalari({}) }}
                      className={`rounded-lg py-2 text-sm font-semibold transition ${sekme === s ? 'bg-white text-deniz-900 shadow' : 'text-slate-500'}`}>
                {s === 'giris' ? 'Giriş yap' : 'Hesap oluştur'}
              </button>
            ))}
          </div>

          <form onSubmit={gonder} className="mt-6 space-y-4">
            {sekme === 'giris' ? (
              <>
                <Alan etiket="E-posta veya kullanıcı adı">
                  <input value={kullaniciAdi} onChange={(e) => setKullaniciAdi(e.target.value)}
                         autoComplete="username" required className={girdiStili} />
                </Alan>
                <Alan etiket="Şifre">
                  <input type="password" value={sifre} onChange={(e) => setSifre(e.target.value)}
                         autoComplete="current-password" required className={girdiStili} />
                </Alan>
              </>
            ) : (
              <>
                <div className="grid grid-cols-2 gap-3">
                  {kayitAlani('ad', 'Ad')}
                  {kayitAlani('soyad', 'Soyad')}
                </div>
                {kayitAlani('kimlikNo', 'TC kimlik veya pasaport no')}
                {kayitAlani('eposta', 'E-posta', 'email')}
                {kayitAlani('telefon', 'Telefon (isteğe bağlı)', 'tel')}
                {kayitAlani('sifre', 'Şifre (en az 8 karakter, harf ve rakam)', 'password')}
              </>
            )}

            {hata && (
              <p className="flex items-center gap-2 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
                <AlertCircle size={16} /> {hata}
              </p>
            )}

            <button type="submit" disabled={bekliyor}
                    className="flex w-full items-center justify-center gap-2 rounded-xl bg-deniz-700 py-3.5 font-semibold text-white hover:bg-deniz-800 disabled:opacity-60">
              {bekliyor && <Loader2 size={18} className="animate-spin" />}
              {sekme === 'giris' ? 'Giriş yap' : 'Hesabı oluştur'}
            </button>
          </form>

          {sekme === 'giris' && (
            <div className="mt-8 rounded-xl bg-kum-100 p-4 text-sm text-slate-600">
              <p className="font-semibold text-slate-700">Deneme hesapları</p>
              <p className="mt-1">Misafir: ayse.yilmaz@example.com · Misafir123!</p>
              <p>Resepsiyon: resepsiyon · Resepsiyon123!</p>
              <p>Yönetici: yonetici · Yonetici123!</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

const girdiStili =
  'w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-deniz-600 focus:ring-4 focus:ring-deniz-100'

function Alan({ etiket, hata, children }: { etiket: string; hata?: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-slate-700">{etiket}</span>
      {children}
      {hata && <span className="mt-1 block text-sm text-red-600">{hata}</span>}
    </label>
  )
}