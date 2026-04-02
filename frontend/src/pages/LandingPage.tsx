import { Link } from 'react-router-dom'
import { Car, Shield, QrCode, BarChart3, CheckCircle, Fuel } from 'lucide-react'

const features = [
  { icon: QrCode, title: 'QR-Based Verification', desc: 'Unique encrypted QR codes for every vehicle' },
  { icon: Shield, title: 'Fraud Prevention', desc: 'JWT tokens + GPS geofencing eliminate spoofing' },
  { icon: BarChart3, title: 'Real-time Analytics', desc: 'Live quota tracking and consumption analytics' },
  { icon: CheckCircle, title: '24L Weekly Quota', desc: 'Automatic weekly reset every Sunday at 00:00' },
]

export default function LandingPage() {
  return (
    <div>
      {/* Hero */}
      <section className="bg-gradient-to-br from-brand-700 via-brand-600 to-brand-800 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-20 sm:py-28 text-center">
          <div className="inline-flex items-center gap-2 bg-white/10 text-white/90 text-sm font-medium px-4 py-1.5 rounded-full mb-6">
            <Fuel className="h-4 w-4" /> National Fuel Management Platform
          </div>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold leading-tight mb-6">
            Automated Fuel Quota System
          </h1>
          <p className="text-xl text-blue-100 max-w-2xl mx-auto mb-10">
            A QR-code-driven platform ensuring fair, transparent fuel distribution for every registered vehicle.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              to="/register"
              className="inline-flex items-center justify-center gap-2 bg-white text-brand-700 font-semibold px-8 py-3.5 rounded-xl hover:bg-blue-50 transition-colors shadow-lg"
            >
              <Car className="h-5 w-5" />
              Register Your Vehicle
            </Link>
            <Link
              to="/login"
              className="inline-flex items-center justify-center gap-2 bg-brand-500/30 text-white font-semibold px-8 py-3.5 rounded-xl hover:bg-brand-500/50 transition-colors border border-white/20"
            >
              Vehicle Owner Login
            </Link>
          </div>
        </div>
      </section>

      {/* Portal Cards */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 py-16">
        <h2 className="text-2xl font-bold text-center text-gray-900 mb-8">Access Your Portal</h2>
        <div className="grid md:grid-cols-2 gap-6 max-w-3xl mx-auto">
          {/* Customer Portal */}
          <div className="card border-2 border-brand-100 hover:border-brand-300 transition-colors group">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-brand-50 rounded-xl">
                <Car className="h-7 w-7 text-brand-600" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-lg">Vehicle Owner Portal</h3>
                <p className="text-sm text-gray-500">For registered vehicle owners</p>
              </div>
            </div>
            <ul className="space-y-2 text-sm text-gray-600 mb-6">
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> View & manage your fuel quota</li>
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> Download your unique QR code</li>
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> Track transaction history</li>
            </ul>
            <div className="flex gap-2">
              <Link to="/login" className="btn-primary flex-1 text-center text-sm py-2">Sign In</Link>
              <Link to="/register" className="btn-secondary flex-1 text-center text-sm py-2">Register</Link>
            </div>
          </div>

          {/* Admin Portal */}
          <div className="card border-2 border-gray-100 hover:border-gray-300 transition-colors group">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-gray-100 rounded-xl">
                <Shield className="h-7 w-7 text-gray-600" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-lg">Admin Dashboard</h3>
                <p className="text-sm text-gray-500">For system administrators</p>
              </div>
            </div>
            <ul className="space-y-2 text-sm text-gray-600 mb-6">
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> Manage vehicles & quotas</li>
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> Oversee fuel stations & reps</li>
              <li className="flex items-center gap-2"><CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" /> Analytics & audit logs</li>
            </ul>
            <Link to="/admin/login" className="btn-secondary w-full text-center text-sm py-2">
              Admin Login
            </Link>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="bg-gray-50 border-t border-gray-100 py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <h2 className="text-2xl font-bold text-center text-gray-900 mb-10">How It Works</h2>
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

