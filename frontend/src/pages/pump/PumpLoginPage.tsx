import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Fuel, Loader2, BadgeCheck } from 'lucide-react'
import toast from 'react-hot-toast'
import { pumpRepLogin } from '@/api/pumpApi'
import { savePumpSession } from '@/layouts/PumpRepLayout'

export default function PumpLoginPage() {
  const navigate = useNavigate()
  const [employeeId, setEmployeeId] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!employeeId.trim()) {
      toast.error('Please enter your employee code')
      return
    }
    setLoading(true)
    try {
      const session = await pumpRepLogin({ employeeId: employeeId.trim() })
      savePumpSession(session)
      toast.success(`Welcome, ${session.name}!`)
      navigate('/pump/scan')
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? err?.response?.data ?? 'Login failed. Check your employee code.'
      toast.error(typeof msg === 'string' ? msg : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] gap-8">
      {/* Brand */}
      <div className="text-center space-y-2">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-brand-600 shadow-lg mb-2">
          <Fuel className="h-8 w-8 text-white" />
        </div>
        <h1 className="text-2xl font-bold text-gray-900">Pump Representative</h1>
        <p className="text-gray-500 text-sm">Sign in with your employee code to start dispensing fuel</p>
      </div>

      {/* Login card */}
      <div className="card w-full max-w-sm space-y-5">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label" htmlFor="emp-code">
              Employee Code
            </label>
            <input
              id="emp-code"
              type="text"
              className="input-field text-center text-lg tracking-widest font-mono uppercase"
              placeholder="e.g. EMP-001"
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value)}
              autoFocus
              autoComplete="off"
            />
          </div>
          <button
            type="submit"
            disabled={loading || !employeeId.trim()}
            className="btn-primary w-full py-3 text-base gap-2"
          >
            {loading ? (
              <Loader2 className="h-5 w-5 animate-spin" />
            ) : (
              <BadgeCheck className="h-5 w-5" />
            )}
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>
      </div>

      <p className="text-xs text-gray-400 text-center max-w-xs">
        Your employee code was issued by your station manager. Contact admin if you have trouble logging in.
      </p>
    </div>
  )
}

