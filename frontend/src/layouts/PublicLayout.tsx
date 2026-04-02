import { Link, Outlet, useLocation } from 'react-router-dom'
import { Fuel } from 'lucide-react'

export default function PublicLayout() {
  const { pathname } = useLocation()

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white border-b border-gray-200 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 font-bold text-brand-700 text-lg">
            <Fuel className="h-6 w-6 text-brand-600" />
            <span>Fuel Quota System</span>
          </Link>
          <nav className="flex items-center gap-3">
            {pathname !== '/login' && pathname !== '/register' && (
              <>
                <Link
                  to="/login"
                  className="text-sm font-medium text-gray-600 hover:text-brand-700 px-3 py-1.5 rounded-lg hover:bg-brand-50 transition-colors"
                >
                  Vehicle Owner Login
                </Link>
                <Link
                  to="/pump"
                  className="text-sm font-medium text-gray-600 hover:text-brand-700 px-3 py-1.5 rounded-lg hover:bg-brand-50 transition-colors"
                >
                  Pump Rep
                </Link>
                <Link
                  to="/admin/login"
                  className="text-sm font-medium text-white bg-brand-600 px-3 py-1.5 rounded-lg hover:bg-brand-700 transition-colors"
                >
                  Admin Portal
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="bg-white border-t border-gray-200 py-4 text-center text-xs text-gray-400">
        © {new Date().getFullYear()} Automated Fuel Quota System · Red e Digital
      </footer>
    </div>
  )
}

