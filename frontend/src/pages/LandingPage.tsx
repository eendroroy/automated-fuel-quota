import { Link } from 'react-router-dom'
import { Car, Shield, QrCode, BarChart3, CheckCircle, Fuel } from 'lucide-react'
import { useTranslation } from 'react-i18next'

export default function LandingPage() {
  const { t } = useTranslation()

  const features = [
    { icon: QrCode, title: t('landing.features.qrVerification'), desc: t('landing.features.qrVerificationDesc') },
    { icon: Shield, title: t('landing.features.fraudPrevention'), desc: t('landing.features.fraudPreventionDesc') },
    { icon: BarChart3, title: t('landing.features.realTimeAnalytics'), desc: t('landing.features.realTimeAnalyticsDesc') },
    { icon: CheckCircle, title: t('landing.features.weeklyQuota'), desc: t('landing.features.weeklyQuotaDesc') },
  ]

  return (
    <div>
      {/* Hero */}
      <section className="bg-gradient-to-br from-brand-700 via-brand-600 to-brand-800 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-20 sm:py-28 text-center">
          <div className="inline-flex items-center gap-2 bg-white/10 text-white/90 text-sm font-medium px-4 py-1.5 rounded-full mb-6">
            <Fuel className="h-4 w-4" /> {t('nav.nationalFuelPlatform')}
          </div>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold leading-tight mb-6">
            {t('landing.heroTitle')}
          </h1>
          <p className="text-xl text-blue-100 max-w-2xl mx-auto mb-10">
            {t('landing.heroSubtitle')}
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              to="/register"
              className="inline-flex items-center justify-center gap-2 bg-white text-brand-700 font-semibold px-8 py-3.5 rounded-xl hover:bg-blue-50 transition-colors shadow-lg"
            >
              <Car className="h-5 w-5" />
              {t('landing.registerVehicle')}
            </Link>
            <Link
              to="/login"
              className="inline-flex items-center justify-center gap-2 bg-brand-500/30 text-white font-semibold px-8 py-3.5 rounded-xl hover:bg-brand-500/50 transition-colors border border-white/20"
            >
              {t('landing.vehicleOwnerLogin')}
            </Link>
          </div>
        </div>
      </section>

      {/* Portal Cards */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 py-16">
        <h2 className="text-2xl font-bold text-center text-gray-900 mb-8">{t('landing.accessPortal')}</h2>
        <div className="grid md:grid-cols-2 gap-6 max-w-3xl mx-auto">
          {/* Customer Portal */}
          <div className="card border-2 border-brand-100 hover:border-brand-300 transition-colors group">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-brand-50 rounded-xl">
                <Car className="h-7 w-7 text-brand-600" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-lg">{t('landing.vehicleOwnerPortal')}</h3>
                <p className="text-sm text-gray-500">{t('landing.vehicleOwnerPortalDesc')}</p>
              </div>
            </div>
            <ul className="space-y-2 text-sm text-gray-600 mb-6">
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> {t('landing.viewManageQuota')}</li>
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> {t('landing.downloadQrCode')}</li>
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> {t('landing.trackHistory')}</li>
            </ul>
            <div className="flex gap-2">
              <Link to="/login" className="btn-primary flex-1 text-center text-sm py-2">{t('common.signIn')}</Link>
              <Link to="/register" className="btn-secondary flex-1 text-center text-sm py-2">{t('common.register')}</Link>
            </div>
          </div>

          {/* Admin Portal */}
          <div className="card border-2 border-gray-100 hover:border-gray-300 transition-colors group">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-gray-100 rounded-xl">
                <Shield className="h-7 w-7 text-gray-600" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-lg">{t('landing.adminDashboard')}</h3>
                <p className="text-sm text-gray-500">{t('landing.adminDashboardDesc')}</p>
              </div>
            </div>
            <ul className="space-y-2 text-sm text-gray-600 mb-6">
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> {t('landing.manageVehiclesQuotas')}</li>
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> {t('landing.overseeStations')}</li>
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> {t('landing.analyticsAudit')}</li>
            </ul>
            <Link to="/admin/login" className="btn-secondary w-full text-center text-sm py-2">
              {t('landing.adminLogin')}
            </Link>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="bg-gray-50 border-t border-gray-100 py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <h2 className="text-2xl font-bold text-center text-gray-900 mb-10">{t('landing.howItWorks')}</h2>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map(({ icon: Icon, title, desc }) => (
              <div key={title} className="card text-center">
                <div className="inline-flex p-3 bg-brand-50 rounded-xl mb-4">
                  <Icon className="h-6 w-6 text-brand-600" />
                </div>
                <h3 className="font-semibold text-gray-900 mb-1">{title}</h3>
                <p className="text-sm text-gray-500">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}
