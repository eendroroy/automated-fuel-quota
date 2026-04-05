import { useEffect, useState } from 'react'
import { Car, MapPin, Droplets, Users, Activity, Clock } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { getAdminStats } from '@/api/adminStatsApi'
import StatsCard from '@/components/common/StatsCard'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import {
  LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import { useTheme } from '@/context/ThemeContext'
import type { AdminStats } from '@/types'

export default function AdminDashboardPage() {
  const { t } = useTranslation()
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const [stats, setStats] = useState<AdminStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getAdminStats()
      .then(setStats)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center h-64"><LoadingSpinner size="lg" /></div>

  // Fallback demo data if API not available yet
  const dailyTx = stats?.dailyTransactions ?? [
    { date: 'Mon', count: 1200 }, { date: 'Tue', count: 1450 }, { date: 'Wed', count: 980 },
    { date: 'Thu', count: 1600 }, { date: 'Fri', count: 2100 }, { date: 'Sat', count: 1850 }, { date: 'Sun', count: 900 },
  ]
  const quotaByClass = stats?.quotaUsageByVehicleClass ?? [
    { vehicleClass: 'Private Car', avgUsed: 18.2 }, { vehicleClass: 'Motorcycle', avgUsed: 8.5 },
    { vehicleClass: 'Bus', avgUsed: 22.1 }, { vehicleClass: 'Truck', avgUsed: 20.8 }, { vehicleClass: 'Three-Wheeler', avgUsed: 12.3 },
  ]

  const chartGridColor = isDark ? '#374151' : '#f3f4f6'
  const chartAxisColor = isDark ? '#9ca3af' : '#6b7280'
  const chartTooltipStyle = isDark
    ? { borderRadius: '8px', fontSize: '13px', border: '1px solid #374151', backgroundColor: '#1f2937', color: '#f9fafb' }
    : { borderRadius: '8px', fontSize: '13px', border: '1px solid #e5e7eb' }

  const cards = [
    { title: t('adminDashboard.totalVehicles'), value: (stats?.totalVehicles ?? 24_850).toLocaleString(), icon: Car, iconBg: 'bg-blue-50 dark:bg-blue-900/30', iconColor: 'text-blue-600 dark:text-blue-400' },
    { title: t('adminDashboard.unverifiedVehicles'), value: stats?.unverifiedVehicles ?? 0, icon: Clock, iconBg: 'bg-yellow-50 dark:bg-yellow-900/30', iconColor: 'text-yellow-600 dark:text-yellow-400' },
    { title: t('adminDashboard.transactionsToday'), value: (stats?.transactionsToday ?? 2_134).toLocaleString(), icon: Activity, iconBg: 'bg-green-50 dark:bg-green-900/30', iconColor: 'text-green-600 dark:text-green-400' },
    { title: t('adminDashboard.activeStations'), value: stats?.activeStations ?? 428, icon: MapPin, iconBg: 'bg-purple-50 dark:bg-purple-900/30', iconColor: 'text-purple-600 dark:text-purple-400' },
    { title: t('adminDashboard.avgQuotaUsed'), value: `${stats?.averageQuotaUsedPercent?.toFixed(1) ?? '64.3'}%`, icon: Droplets, iconBg: 'bg-orange-50 dark:bg-orange-900/30', iconColor: 'text-orange-600 dark:text-orange-400' },
    { title: t('adminDashboard.totalTxWeek'), value: (stats?.totalTransactionsThisWeek ?? 14_820).toLocaleString(), icon: Users, iconBg: 'bg-pink-50 dark:bg-pink-900/30', iconColor: 'text-pink-600 dark:text-pink-400' },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('adminDashboard.title')}</h1>
        <p className="text-gray-500 dark:text-gray-400 text-sm mt-0.5">{t('common.loading').replace('...', '')} </p>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        {cards.map((c) => (
          <StatsCard key={c.title} title={c.title} value={c.value} icon={c.icon} iconBg={c.iconBg} iconColor={c.iconColor} />
        ))}
      </div>

      {/* Charts */}
      <div className="grid lg:grid-cols-2 gap-6">
        {/* Daily transactions */}
        <div className="card">
          <h3 className="font-semibold text-gray-900 dark:text-white mb-5">{t('adminDashboard.dailyTrend')}</h3>
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={dailyTx} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke={chartGridColor} />
              <XAxis dataKey="date" tick={{ fontSize: 12, fill: chartAxisColor }} />
              <YAxis tick={{ fontSize: 12, fill: chartAxisColor }} />
              <Tooltip contentStyle={chartTooltipStyle} />
              <Line type="monotone" dataKey="count" name="Transactions" stroke="#2563eb" strokeWidth={2.5} dot={{ r: 4 }} activeDot={{ r: 6 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Quota usage by class */}
        <div className="card">
          <h3 className="font-semibold text-gray-900 dark:text-white mb-5">{t('adminDashboard.quotaUsageByClass')}</h3>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={quotaByClass} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke={chartGridColor} />
              <XAxis dataKey="vehicleClass" tick={{ fontSize: 10, fill: chartAxisColor }} />
              <YAxis domain={[0, 24]} tick={{ fontSize: 12, fill: chartAxisColor }} />
              <Tooltip contentStyle={chartTooltipStyle} />
              <Legend wrapperStyle={{ fontSize: '12px' }} />
              <Bar dataKey="avgUsed" name="Avg Used (L)" fill="#2563eb" radius={[4, 4, 0, 0]} maxBarSize={48} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  )
}
