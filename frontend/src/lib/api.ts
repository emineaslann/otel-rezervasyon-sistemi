/**
 * Backend'e istek gönderen tek merkez.
 * - Token varsa her isteğe "Authorization: Bearer ..." ekler
 * - Hata cevaplarını (GlobalHataYakalayici'nin JSON'u) ApiHatasi'na çevirir
 * - 401 gelirse oturumu düşürür
 */
const TOKEN_ANAHTARI = 'kiyi_otel_token'

export class ApiHatasi extends Error {
  durum: number
  alanlar: Record<string, string> | null
  constructor(durum: number, mesaj: string, alanlar: Record<string, string> | null = null) {
    super(mesaj)
    this.durum = durum
    this.alanlar = alanlar
  }
}

export const tokenAl = () => localStorage.getItem(TOKEN_ANAHTARI)

export function tokenKaydet(token: string | null) {
  if (token) localStorage.setItem(TOKEN_ANAHTARI, token)
  else localStorage.removeItem(TOKEN_ANAHTARI)
}

async function istek<T>(yontem: string, yol: string, govde?: unknown): Promise<T> {
  const basliklar: Record<string, string> = {}
  const token = tokenAl()
  if (token) basliklar.Authorization = `Bearer ${token}`
  if (govde !== undefined) basliklar['Content-Type'] = 'application/json'

  let cevap: Response
  try {
    cevap = await fetch(yol, {
      method: yontem,
      headers: basliklar,
      body: govde !== undefined ? JSON.stringify(govde) : undefined,
    })
  } catch {
    throw new ApiHatasi(0, 'Sunucuya ulaşılamadı. Bağlantınızı kontrol edin.')
  }

  // Token süresi dolmuş ya da geçersiz: oturumu düşür
  if (cevap.status === 401 && token) {
    tokenKaydet(null)
    window.dispatchEvent(new Event('oturum-dustu'))
  }

  if (!cevap.ok) {
    const g = await cevap.json().catch(() => null)
    throw new ApiHatasi(cevap.status, g?.mesaj ?? 'Beklenmeyen bir hata oluştu.', g?.alanlar ?? null)
  }

  if (cevap.status === 204) return undefined as T
  return cevap.json() as Promise<T>
}

export const api = {
  get: <T>(yol: string) => istek<T>('GET', yol),
  post: <T>(yol: string, govde: unknown = {}) => istek<T>('POST', yol, govde),
  put: <T>(yol: string, govde: unknown = {}) => istek<T>('PUT', yol, govde),
  del: <T = void>(yol: string) => istek<T>('DELETE', yol),
}

/** Eski kullanım için (ana sayfa ve arama sonuçları) */
export const apiGet = <T>(yol: string) => api.get<T>(yol)