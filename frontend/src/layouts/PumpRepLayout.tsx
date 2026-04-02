import { Outlet, useNavigate } from 'react-router-dom'
import { Fuel, LogOut, User } from 'lucide-react'
import type { PumpRepSession } from '@/types'

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
  const session = getPumpSession()

  const handleLogout = () => {
    clearPumpSession()
    navigate('/pump')
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-40 shadow-sm">
        <div className="max-w-lg mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2 font-bold text-brand-700 text-base">
            <Fuel className="h-5 w-5 text-brand-600" />
            <span>Pump Rep Portal</span>
          </div>
          {session && (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1.5 text-sm text-gray-600">
                <User className="h-4 w-4" />
                <span className="hidden sm:inline">{session.name}</span>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center gap-1 text-sm text-red-500 hover:text-red-700 px-2 py-1 rounded hover:bg-red-50 transition-colors"
              >
                <LogOut className="h-4 w-4" />
                <span className="hidden sm:inline">Logout</span>
              </button>
            </div>
          )}
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 max-w-lg mx-auto w-full px-4 py-6">
        <Outlet />
      </main>

      <footer className="py-3 text-center text-xs text-gray-400 border-t border-gray-100">
        © {new Date().getFullYear()} Automated Fuel Quota System
      </footer>
    </div>
  )
}

