import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Fuel, Loader2, BadgeCheck } from 'lucide-react'
import toast from 'react-hot-toast'
import { useTranslation } from 'react-i18next'
import { pumpRepLogin } from '@/api/pumpApi'
import { savePumpSession } from '@/layouts/PumpRepLayout'

export default function PumpLoginPage() {
  const navigate = useNavigate()
  const { t } = useTranslation()
  const [mobileNumber, setMobileNumber] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!mobileNumber.trim()) {
      toast.error(t('errors.mobileRequired'))
      return
    }
    if (!/^01[3-9]\d{8}$/.test(mobileNumber.trim())) {
      toast.error(t('errors.mobileInvalid'))
      return
    }
    setLoading(true)
    try {
      const session = await pumpRepLogin({ mobileNumber: mobileNumber.trim() })
      savePumpSession(session)
      toast.success(t('dashboard.welcomeBack', { name: session.name }))
      navigate('/pump/scan')
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? err?.response?.data ?? t('errors.loginFailed')
      toast.error(typeof msg === 'string' ? msg : t('errors.loginFailed'))
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
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('pumpLogin.title')}</h1>
        <p className="text-gray-500 dark:text-gray-400 text-sm">{t('pumpLogin.subtitle')}</p>
      </div>

      {/* Login card */}
      <div className="card w-full max-w-sm space-y-5">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label" htmlFor="mobile">
              {t('pumpLogin.mobileLabel')}
            </label>
            <input
              id="mobile"
              type="tel"
              className="input-field text-center text-lg tracking-widest font-mono"
              placeholder={t('pumpLogin.mobilePlaceholder')}
              value={mobileNumber}
              onChange={(e) => setMobileNumber(e.target.value)}
              autoFocus
              autoComplete="tel"
              maxLength={11}
              inputMode="numeric"
            />
          </div>
          <button
            type="submit"
            disabled={loading || !mobileNumber.trim()}
            className="btn-primary w-full py-3 text-base gap-2"
          >
            {loading ? (
              <Loader2 className="h-5 w-5 animate-spin" />
            ) : (
              <BadgeCheck className="h-5 w-5" />
            )}
            {loading ? t('pumpLogin.signingIn') : t('pumpLogin.signIn')}
          </button>
        </form>
      </div>

      <p className="text-xs text-gray-400 text-center max-w-xs">
        {t('pumpLogin.helpText')}
      </p>
    </div>
  )
}
