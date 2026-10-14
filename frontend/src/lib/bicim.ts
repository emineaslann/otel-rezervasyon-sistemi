/** 11880 -> "11.880 ₺" */
export function para(tutar: number): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    maximumFractionDigits: 0,
  }).format(tutar)
}

/** Bugünden n gün sonrasını "2026-10-21" biçiminde verir (yerel saatle). */
export function isoTarih(gunSonra = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + gunSonra)
  const ay = String(d.getMonth() + 1).padStart(2, '0')
  const gun = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${ay}-${gun}`
}

/** "2026-10-21" -> "21 Eki 2026" */
export function kisaTarih(iso: string): string {
  return new Date(iso + 'T00:00:00').toLocaleDateString('tr-TR', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
}

/** İki tarih arasındaki gece sayısı */
export function geceSayisi(giris: string, cikis: string): number {
  const fark = new Date(cikis + 'T00:00:00').getTime() - new Date(giris + 'T00:00:00').getTime()
  return Math.round(fark / 86_400_000)
}

/** Oda tipinin fotoğrafı */
export function odaGorseli(tipAdi: string): string {
  const harita: Record<string, string> = {
    Standart: '/img/standart.jpg',
    Deluxe: '/img/deluxe.jpg',
    Aile: '/img/aile.jpg',
    Suit: '/img/suit.jpg',
  }
  return harita[tipAdi] ?? '/img/hero.jpg'
}