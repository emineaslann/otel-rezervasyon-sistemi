import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { Loader2, ShieldAlert } from 'lucide-react'
import { useOturum } from '../context/OturumContext'
import type { Rol } from '../lib/tipler'

/**
 * Giriş yapılmamışsa giriş sayfasına yönlendirir (sonra buraya geri döner).
 * roller verilirse sadece o rollere izin verir.
 */
export default function GirisGerekli({ roller, children }: { roller?: Rol[]; children: ReactNode }) {
  const { kullanici, yukleniyor } = useOturum()
  const konum = useLocation()

  // Sayfa yenilendiğinde oturum backend'den doğrulanana kadar bekle
  if (yukleniyor) {
    return (
      <div className="grid min-h-[50vh] place-items-center text-slate-400">
        <Loader2 className="animate-spin" size={32} />
      </div>
    )
  }

  if (!kullanici) {
    const donus = encodeURIComponent(konum.pathname + konum.search)
    return <Navigate to={`/giris?donus=${donus}`} replace />
  }

  if (roller && !roller.includes(kullanici.rol)) {
    return (
      <div className="mx-auto max-w-md px-6 py-24 text-center">
        <ShieldAlert className="mx-auto text-red-500" size={40} />
        <p className="mt-4 font-serif text-2xl text-deniz-950">Bu sayfaya erişim yetkiniz yok</p>
      </div>
    )
  }

  return <>{children}</>
}