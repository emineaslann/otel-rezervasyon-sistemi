import { Waves, MapPin, Phone, Mail } from 'lucide-react'

export default function Altbilgi() {
  return (
    <footer className="mt-24 bg-deniz-950 text-deniz-100">
      <div className="mx-auto grid max-w-6xl gap-10 px-6 py-14 md:grid-cols-3">
        <div>
          <div className="flex items-center gap-2">
            <Waves size={20} className="text-pirinc-400" />
            <span className="font-serif text-2xl text-white">Kıyı Otel</span>
          </div>
          <p className="mt-4 max-w-xs text-sm text-deniz-200">
            Denize sıfır 32 oda, spa ve sabah kahvaltısıyla sakin bir tatil.
          </p>
        </div>
        <div className="space-y-3 text-sm">
          <p className="font-semibold text-white">İletişim</p>
          <p className="flex items-center gap-2"><MapPin size={16} /> Sahil Yolu No: 1</p>
          <p className="flex items-center gap-2"><Phone size={16} /> 0 (212) 000 00 00</p>
          <p className="flex items-center gap-2"><Mail size={16} /> rezervasyon@kiyiotel.example</p>
        </div>
        <div className="space-y-3 text-sm">
          <p className="font-semibold text-white">Konaklama</p>
          <p>Giriş saati: 14:00</p>
          <p>Çıkış saati: 12:00</p>
          <p>Ücretsiz iptal: girişten önce</p>
        </div>
      </div>
      <div className="border-t border-white/10 py-5 text-center text-xs text-deniz-300">
        © 2026 Kıyı Otel · Veritabanı Yönetim Sistemleri dersi projesi
      </div>
    </footer>
  )
}