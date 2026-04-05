import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Fuel, LogIn } from 'lucide-react'
import toast from 'react-hot-toast'
import { useTranslation } from 'react-i18next'
import { customerLogin } from '@/api/authApi'
import { useAuth } from '@/context/AuthContext'
import LoadingSpinner from '@/components/common/LoadingSpinner'

export default function CustomerLoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const [form, setForm] = useState({ mobileNumber: '', password: '' })
  const [showPwd, setShowPwd] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.mobileNumber || !form.password) {
      toast.error(t('errors.fillAllFields'))
      return
    }
    if (!/^01[3-9]\d{8}$/.test(form.mobileNumber)) {
      toast.error(t('errors.mobileInvalid'))
      return
    }
    setLoading(true)
    try {
      const res = await customerLogin(form)
      login(res.token, res.user)
      toast.success(t('dashboard.welcomeBack', { name: res.user.name.split(' ')[0] }))
      navigate('/dashboard')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? t('errors.invalidCredentials'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center h-14 w-14 bg-brand-50 rounded-2xl mb-4">
            <Fuel className="h-7 w-7 text-brand-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">{t('customerLogin.title')}</h1>
          <p className="text-gray-500 text-sm mt-1">{t('customerLogin.subtitle')}</p>
        </div>

        <div className="card">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label">{t('customerLogin.mobileLabel')}</label>
              <input
                type="tel"
                className="input-field"
                placeholder={t('customerLogin.mobilePlaceholder')}
                value={form.mobileNumber}
                onChange={(e) => setForm((f) => ({ ...f, mobileNumber: e.target.value }))}
                autoComplete="tel"
                maxLength={11}
                inputMode="numeric"
              />
            </div>

            <div>
              <label className="label">{t('customerLogin.passwordLabel')}</label>
              <div className="relative">
                <input
                  type={showPwd ? 'text' : 'password'}
                  className="input-field pr-10"
                  placeholder={t('customerLogin.passwordPlaceholder')}
                  value={form.password}
                  onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPwd((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showPwd ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <button type="submit" disabled={loading} className="btn-primary w-full gap-2 py-2.5">
              {loading ? <LoadingSpinner size="sm" /> : <LogIn className="h-4 w-4" />}
              {t('common.signIn')}
            </button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-5">
            {t('customerLogin.noAccount')}{' '}
            <Link to="/register" className="text-brand-600 font-medium hover:underline">
              {t('customerLogin.registerNow')}
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
