import { Link, Outlet, NavLink, useNavigate } from 'react-router-dom'
import { Fuel, LayoutDashboard, QrCode, History, LogOut, User, Car, FileCheck } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useTranslation } from 'react-i18next'
import ThemeToggle from '@/components/common/ThemeToggle'
import LanguageSwitcher from '@/components/common/LanguageSwitcher'
import toast from 'react-hot-toast'

export default function CustomerLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const { t } = useTranslation()

  const navItems = [
    { to: '/dashboard', icon: LayoutDashboard, label: t('nav.dashboard') },
    { to: '/vehicles', icon: Car, label: t('nav.myVehicles') },
    { to: '/claims', icon: FileCheck, label: t('nav.claims') },
    { to: '/qr-code', icon: QrCode, label: t('nav.myQrCode') },
    { to: '/transactions', icon: History, label: t('nav.transactions') },
  ]

  const handleLogout = () => {
    logout()
    toast.success(t('common.signOut'))
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900">
      {/* Top Nav */}
      <header className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2 font-bold text-brand-700 dark:text-brand-400 text-lg">
            <Fuel className="h-6 w-6 text-brand-600" />
            <span className="hidden sm:inline">{t('common.appNameShort')}</span>
          </Link>

          <nav className="hidden md:flex items-center gap-1">
            {navItems.map(({ to, icon: Icon, label }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  `flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-brand-50 text-brand-700 dark:bg-brand-900/50 dark:text-brand-400'
                      : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-gray-100'
                  }`
                }
              >
                <Icon className="h-4 w-4" />
                {label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2 sm:gap-3">
            <div className="hidden sm:flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
              <div className="h-8 w-8 rounded-full bg-brand-100 dark:bg-brand-900 flex items-center justify-center">
                <User className="h-4 w-4 text-brand-600 dark:text-brand-400" />
              </div>
              <span className="hidden lg:inline font-medium max-w-[120px] truncate">{user?.name}</span>
            </div>
            <LanguageSwitcher />
            <ThemeToggle />
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400 hover:text-red-600 dark:hover:text-red-400 px-2 py-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-950/20 transition-colors"
              title={t('common.logout')}
            >
              <LogOut className="h-4 w-4" />
              <span className="hidden lg:inline">{t('common.logout')}</span>
            </button>
          </div>
        </div>

        {/* Mobile nav - Bottom Tab Bar */}
        <div className="md:hidden fixed bottom-0 left-0 right-0 bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-700 px-2 py-1 z-50">
          <div className="flex items-center justify-around">
            {navItems.map(({ to, icon: Icon, label }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  `flex flex-col items-center gap-0.5 py-2 px-3 rounded-lg text-xs font-medium transition-colors min-w-0 flex-1 ${
                    isActive
                      ? 'text-brand-700 dark:text-brand-400 bg-brand-50 dark:bg-brand-900/50'
                      : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                  }`
                }
              >
                <Icon className="h-5 w-5" />
                <span className="truncate text-[10px]">{label}</span>
              </NavLink>
            ))}
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 py-4 sm:py-8 pb-20 md:pb-8">
        <Outlet />
      </main>
    </div>
  )
}
