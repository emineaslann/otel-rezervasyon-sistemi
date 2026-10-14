import { Link, useNavigate } from 'react-router'
import { Waves, CalendarCheck, LogOut, LayoutDashboard } from 'lucide-react'
import { useOturum } from '../context/OturumContext'

export default function UstMenu() {
  const { kullanici, cikis } = useOturum()
  const navigate = useNavigate()
  const personel = kullanici && kullanici.rol !== 'MISAFIR'

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
        <Link to="/" className="flex items-center gap-2">
          <span className="grid h-9 w-9 place-items-center rounded-full bg-deniz-700 text-white">
            <Waves size={18} />
          </span>
          <span className="font-serif text-2xl font-semibold text-deniz-900">Kıyı Otel</span>
        </Link>

        <nav className="hidden items-center gap-8 text-sm font-medium text-slate-600 md:flex">
          <a href="/#odalar" className="hover:text-deniz-700">Odalar</a>
          <a href="/#olanaklar" className="hover:text-deniz-700">Olanaklar</a>
        </nav>

        {kullanici ? (
          <div className="flex items-center gap-2">
            <Link to={personel ? (kullanici.rol === 'YONETICI' ? '/panel/yonetim' : '/panel/resepsiyon') : '/hesabim'}
                  className="hidden items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 sm:flex">
              {personel ? <LayoutDashboard size={16} /> : <CalendarCheck size={16} />}
              {personel ? 'Panel' : 'Rezervasyonlarım'}
            </Link>
            <span className="flex items-center gap-2 rounded-full bg-deniz-50 py-1 pl-1 pr-4 text-sm font-semibold text-deniz-900">
              <span className="grid h-8 w-8 place-items-center rounded-full bg-deniz-700 text-white">
                {kullanici.adSoyad.charAt(0)}
              </span>
              {kullanici.adSoyad.split(' ')[0]}
            </span>
            <button onClick={() => { cikis(); navigate('/') }} title="Çıkış yap"
                    className="grid h-9 w-9 place-items-center rounded-full text-slate-500 hover:bg-slate-100 hover:text-red-600">
              <LogOut size={18} />
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Link to="/giris?donus=/hesabim"
                  className="hidden items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 sm:flex">
              <CalendarCheck size={16} /> Rezervasyonlarım
            </Link>
            <Link to="/giris"
                  className="rounded-full bg-deniz-700 px-5 py-2 text-sm font-semibold text-white hover:bg-deniz-800">
              Giriş yap
            </Link>
          </div>
        )}
      </div>
    </header>
  )
}