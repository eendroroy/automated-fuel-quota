import { Link, Outlet, useLocation } from 'react-router-dom'
import { Fuel } from 'lucide-react'
import ThemeToggle from '@/components/common/ThemeToggle'

export default function PublicLayout() {
  const { pathname } = useLocation()

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 font-bold text-brand-700 dark:text-brand-400 text-lg">
            <Fuel className="h-6 w-6 text-brand-600 dark:text-brand-500" />
            <span>Fuel Quota System</span>
          </Link>
          <nav className="flex items-center gap-3">
            {pathname !== '/login' && pathname !== '/register' && (
              <>
                <Link
                  to="/login"
                  className="text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-brand-700 dark:hover:text-brand-400 px-3 py-1.5 rounded-lg hover:bg-brand-50 dark:hover:bg-gray-800 transition-colors"
                >
                  Vehicle Owner Login
                </Link>
                <Link
                  to="/pump"
                  className="text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-brand-700 dark:hover:text-brand-400 px-3 py-1.5 rounded-lg hover:bg-brand-50 dark:hover:bg-gray-800 transition-colors"
                >
                  Pump Rep
                </Link>
                <Link
                  to="/admin/login"
                  className="text-sm font-medium text-white bg-brand-600 dark:bg-brand-700 px-3 py-1.5 rounded-lg hover:bg-brand-700 dark:hover:bg-brand-800 transition-colors"
                >
                  Admin Portal
                </Link>
              </>
            )}
            <ThemeToggle />
          </nav>
        </div>
      </header>
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-800 py-4 text-center text-xs text-gray-400 dark:text-gray-500">
        © {new Date().getFullYear()} Automated Fuel Quota System · Red e Digital
      </footer>
    </div>
  )
}

