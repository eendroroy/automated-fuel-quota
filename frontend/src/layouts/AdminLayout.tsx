import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  Fuel, LayoutDashboard, Car, MapPin, Droplets, Users, ClipboardList,
  LogOut, Menu, ChevronRight, Settings, UserCog,
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useTranslation } from 'react-i18next'
import toast from 'react-hot-toast'
import ThemeToggle from '@/components/common/ThemeToggle'
import LanguageSwitcher from '@/components/common/LanguageSwitcher'

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const navItems = [
    { to: '/admin/dashboard', icon: LayoutDashboard, label: t('adminNav.dashboard') },
    { to: '/admin/vehicles', icon: Car, label: t('adminNav.vehicles') },
    { to: '/admin/stations', icon: MapPin, label: t('adminNav.fuelStations') },
    { to: '/admin/quotas', icon: Droplets, label: t('adminNav.quotaManagement') },
    { to: '/admin/quota-config', icon: Settings, label: t('adminNav.quotaConfig') },
    { to: '/admin/pump-reps', icon: Users, label: t('adminNav.pumpRepresentatives') },
    { to: '/admin/users', icon: UserCog, label: t('adminNav.userManagement') },
    { to: '/admin/audit-logs', icon: ClipboardList, label: t('adminNav.auditLogs') },
  ]

  const handleLogout = () => {
    logout()
    toast.success(t('common.signOut'))
    navigate('/admin/login')
  }

  const SidebarContent = () => (
    <>
      {/* Logo */}
      <div className="px-6 py-5 border-b border-gray-700">
        <div className="flex items-center gap-2">
          <Fuel className="h-7 w-7 text-brand-400" />
          <div>
            <p className="text-white font-bold text-base leading-tight">{t('common.appNameShort')}</p>
            <p className="text-gray-400 text-xs">{t('adminNav.adminPortal')}</p>
          </div>
        </div>
      </div>

      {/* Nav items */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {navItems.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            onClick={() => setSidebarOpen(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors group ${
                isActive ? 'bg-brand-600 text-white' : 'text-gray-300 hover:bg-gray-700 hover:text-white'
              }`
            }
          >
            <Icon className="h-5 w-5 flex-shrink-0" />
            <span className="flex-1">{label}</span>
            <ChevronRight className="h-3.5 w-3.5 opacity-0 group-hover:opacity-60 transition-opacity" />
          </NavLink>
        ))}
      </nav>

      {/* User + logout */}
      <div className="px-3 py-4 border-t border-gray-700">
        <div className="flex items-center gap-3 px-3 py-2 mb-1">
          <div className="h-8 w-8 rounded-full bg-brand-500 flex items-center justify-center flex-shrink-0">
            <span className="text-white text-sm font-semibold">
              {user?.name?.charAt(0).toUpperCase()}
            </span>
          </div>
          <div className="min-w-0">
            <p className="text-white text-sm font-medium truncate">{user?.name}</p>
            <p className="text-gray-400 text-xs truncate">{user?.email}</p>
          </div>
        </div>
        <div className="px-3 py-2 flex items-center gap-2">
          <LanguageSwitcher />
          <ThemeToggle />
        </div>
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-300 hover:bg-red-600/20 hover:text-red-400 transition-colors"
        >
          <LogOut className="h-5 w-5" />
          {t('common.signOut')}
        </button>
      </div>
    </>
  )

  return (
    <div className="flex h-screen bg-gray-100 dark:bg-gray-950 overflow-hidden">
      {/* Desktop sidebar */}
      <aside className="hidden lg:flex lg:flex-col w-64 bg-gray-800 flex-shrink-0">
        <SidebarContent />
      </aside>

      {/* Mobile sidebar overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 bg-black/60" onClick={() => setSidebarOpen(false)} />
          <aside className="relative z-10 flex flex-col w-64 h-full bg-gray-800">
            <SidebarContent />
          </aside>
        </div>
      )}

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top bar */}
        <header className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800 h-14 flex items-center px-4 sm:px-6 gap-3 flex-shrink-0">
          <button
            className="lg:hidden text-gray-500 hover:text-gray-700"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex-1" />
          <span className="text-sm text-gray-500 hidden sm:block">
            {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
          </span>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
