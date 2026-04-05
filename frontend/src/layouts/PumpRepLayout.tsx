import { Outlet, useNavigate } from 'react-router-dom'
import { Fuel, LogOut, User } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import type { PumpRepSession } from '@/types'
import ThemeToggle from '@/components/common/ThemeToggle'
import LanguageSwitcher from '@/components/common/LanguageSwitcher'

const PUMP_SESSION_KEY = 'pumpRepSession'

export function getPumpSession(): PumpRepSession | null {
  try {
    const raw = localStorage.getItem(PUMP_SESSION_KEY)
    return raw ? (JSON.parse(raw) as PumpRepSession) : null
  } catch {
    return null
  }
}

export function savePumpSession(session: PumpRepSession) {
  localStorage.setItem(PUMP_SESSION_KEY, JSON.stringify(session))
}

export function clearPumpSession() {
  localStorage.removeItem(PUMP_SESSION_KEY)
}

export default function PumpRepLayout() {
  const navigate = useNavigate()
  const { t } = useTranslation()
  const session = getPumpSession()

  const handleLogout = () => {
    clearPumpSession()
    navigate('/pump')
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900">
      {/* Header */}
      <header className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800 sticky top-0 z-40 shadow-sm">
        <div className="max-w-lg mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2 font-bold text-brand-700 dark:text-brand-400 text-base">
            <Fuel className="h-5 w-5 text-brand-600" />
            <span className="truncate">{t('pumpRepLayout.portalTitle')}</span>
          </div>
          <div className="flex items-center gap-2">
            <LanguageSwitcher />
            {session && (
              <>
                <div className="hidden sm:flex items-center gap-1.5 text-sm text-gray-600 dark:text-gray-300">
                  <User className="h-4 w-4" />
                  <span className="truncate max-w-[100px]">{session.name}</span>
                </div>
                <ThemeToggle />
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-1 text-sm text-red-500 dark:text-red-400 hover:text-red-700 dark:hover:text-red-300 px-2 py-1 rounded hover:bg-red-50 dark:hover:bg-red-950/20 transition-colors"
                  title={t('pumpRepLayout.logout')}
                >
                  <LogOut className="h-4 w-4" />
                  <span className="hidden sm:inline">{t('pumpRepLayout.logout')}</span>
                </button>
              </>
            )}
            {!session && <ThemeToggle />}
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 max-w-lg mx-auto w-full px-4 py-4 sm:py-6">
        <Outlet />
      </main>

      <footer className="py-3 text-center text-xs text-gray-400 dark:text-gray-500 border-t border-gray-100 dark:border-gray-800">
        {t('footer.copyright', { year: new Date().getFullYear() })}
      </footer>
    </div>
  )
}
