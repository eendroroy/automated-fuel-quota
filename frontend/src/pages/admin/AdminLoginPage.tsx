import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Shield, LogIn } from 'lucide-react'
import toast from 'react-hot-toast'
import { useTranslation } from 'react-i18next'
import { adminLogin } from '@/api/authApi'
import { useAuth } from '@/context/AuthContext'
import LoadingSpinner from '@/components/common/LoadingSpinner'

export default function AdminLoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const [form, setForm] = useState({ mobileNumber: '', password: '' })
  const [showPwd, setShowPwd] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.mobileNumber || !form.password) { toast.error(t('errors.fillAllFields')); return }
    setLoading(true)
    try {
      const res = await adminLogin(form)
      login(res.token, res.user)
      toast.success(t('adminDashboard.title'))
      navigate('/admin/dashboard')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? t('errors.invalidAdminCredentials'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center h-14 w-14 bg-gray-100 rounded-2xl mb-4">
            <Shield className="h-7 w-7 text-gray-700" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">{t('adminLogin.title')}</h1>
          <p className="text-gray-500 text-sm mt-1">{t('adminLogin.subtitle')}</p>
        </div>

        <div className="card border-gray-200">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label">{t('adminLogin.emailLabel')}</label>
              <input type="email" className="input-field" placeholder={t('adminLogin.emailPlaceholder')}
                value={form.mobileNumber} onChange={(e) => setForm((f) => ({ ...f, mobileNumber: e.target.value }))} autoComplete="email" />
            </div>
            <div>
              <label className="label">{t('adminLogin.passwordLabel')}</label>
              <div className="relative">
                <input type={showPwd ? 'text' : 'password'} className="input-field pr-10"
                  placeholder={t('adminLogin.passwordPlaceholder')} value={form.password}
                  onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))} autoComplete="current-password" />
                <button type="button" onClick={() => setShowPwd((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                  {showPwd ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>
            <button type="submit" disabled={loading} className="btn-primary w-full gap-2 py-2.5 bg-gray-800 hover:bg-gray-900 focus:ring-gray-700">
              {loading ? <LoadingSpinner size="sm" /> : <LogIn className="h-4 w-4" />}
              {t('adminLogin.signIn')}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
