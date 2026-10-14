import { Outlet } from 'react-router'
import UstMenu from '../components/UstMenu'
import Altbilgi from '../components/Altbilgi'

/** Misafirin gördüğü tüm sayfaların ortak iskeleti */
export default function SiteDuzeni() {
  return (
    <div className="flex min-h-screen flex-col bg-white">
      <UstMenu />
      <main className="flex-1">
        <Outlet />
      </main>
      <Altbilgi />
    </div>
  )
}