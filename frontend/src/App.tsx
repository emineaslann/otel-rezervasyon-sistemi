import { Route, Routes } from 'react-router'
import SiteDuzeni from './layouts/SiteDuzeni'
import AnaSayfa from './pages/AnaSayfa'
import AramaSonuclari from './pages/AramaSonuclari'
import Giris from './pages/Giris'
import Rezervasyon from './pages/Rezervasyon'
import GirisGerekli from './components/GirisGerekli'
import Hesabim from './pages/Hesabim'

const YakindaSayfasi = ({ ad }: { ad: string }) => (
  <div className="mx-auto max-w-6xl px-6 py-24 text-center text-slate-500">{ad} sayfası yakında</div>
)

export default function App() {
  return (
    <Routes>
      {/* Misafirin gördüğü site */}
      <Route element={<SiteDuzeni />}>
        <Route index element={<AnaSayfa />} />
        <Route path="arama" element={<AramaSonuclari />} />
        <Route path="rezervasyon" element={
          <GirisGerekli roller={['MISAFIR']}><Rezervasyon /></GirisGerekli>
        } />
                <Route path="hesabim" element={
          <GirisGerekli roller={['MISAFIR']}><Hesabim /></GirisGerekli>
        } />
      </Route>

      {/* Menüsüz tam ekran sayfa */}
      <Route path="giris" element={<Giris />} />

      {/* Personel panelleri (sonraki adımlarda) */}
      <Route path="panel/resepsiyon" element={<YakindaSayfasi ad="Resepsiyon paneli" />} />
      <Route path="panel/yonetim" element={<YakindaSayfasi ad="Yönetim paneli" />} />
    </Routes>
  )
}