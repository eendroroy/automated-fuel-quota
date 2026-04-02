import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Droplets, QrCode, History, ArrowRight, AlertCircle } from 'lucide-react'
import { getMyQuota } from '@/api/quotaApi'
import { getMyTransactions } from '@/api/transactionApi'
import { getQrToken } from '@/api/vehicleApi'
import { useAuth } from '@/context/AuthContext'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import { formatDateTime, formatLitres } from '@/utils/formatters'
import QRCode from 'react-qr-code'
import type { Quota, Transaction } from '@/types'

// Quota arc gauge
function QuotaGauge({ used, total }: { used: number; total: number }) {
  const pct = Math.min((used / total) * 100, 100)
  const radius = 60
  const circ = 2 * Math.PI * radius
  const dash = (pct / 100) * circ
  const color = pct >= 90 ? '#dc2626' : pct >= 60 ? '#f59e0b' : '#16a34a'

  return (
    <div className="flex flex-col items-center">
      <svg width="160" height="100" viewBox="0 0 160 100">
        {/* Background arc */}
        <circle cx="80" cy="80" r={radius} fill="none" stroke="#e5e7eb" strokeWidth="12"
          strokeDasharray={`${circ * 0.75} ${circ * 0.25}`}
          strokeDashoffset={circ * 0.375}
          strokeLinecap="round" transform="rotate(180 80 80)" />
        {/* Filled arc */}
        <circle cx="80" cy="80" r={radius} fill="none" stroke={color} strokeWidth="12"
          strokeDasharray={`${dash * 0.75} ${circ - dash * 0.75}`}
          strokeDashoffset={circ * 0.375}
          strokeLinecap="round" transform="rotate(180 80 80)"
          style={{ transition: 'stroke-dasharray 0.6s ease' }} />
      </svg>
      <div className="-mt-8 text-center">
        <p className="text-3xl font-bold text-gray-900">{formatLitres(total - used)}</p>
        <p className="text-xs text-gray-500">remaining of {total}L</p>
      </div>
    </div>
  )
}

export default function CustomerDashboardPage() {
  const { user } = useAuth()
  const [quota, setQuota] = useState<Quota | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [qrToken, setQrToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getMyQuota(), getMyTransactions({ page: 0, size: 5 }), getQrToken()])
      .then(([q, t, qr]) => {
        setQuota(q)
        setTransactions(t.content)
        setQrToken(qr.token)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center h-64"><LoadingSpinner size="lg" /></div>

  const limit = quota?.limitLiters ?? 24
  const used = quota?.usedLiters ?? 0
  const pct = Math.round((used / limit) * 100)
  const periodLabel = quota?.period
    ? quota.period.charAt(0) + quota.period.slice(1).toLowerCase()
    : 'Weekly'

  return (
    <div className="space-y-6">
      {/* Welcome */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Welcome, {user?.name?.split(' ')[0]}!</h1>
        <p className="text-gray-500 text-sm mt-0.5">Here's your fuel quota status for this {periodLabel.toLowerCase()} period.</p>
      </div>

      {/* Status warning */}
      {pct >= 90 && (
        <div className="flex items-center gap-3 bg-red-50 border border-red-200 rounded-xl px-4 py-3 text-sm text-red-700">
          <AlertCircle className="h-5 w-5 flex-shrink-0" />
          You've used {pct}% of your {periodLabel.toLowerCase()} quota. Only {formatLitres(limit - used)} remaining.
        </div>
      )}

      <div className="grid md:grid-cols-3 gap-6">
        {/* Quota gauge */}
        <div className="md:col-span-1 card flex flex-col items-center">
          <div className="flex items-center gap-2 mb-4 self-start">
            <Droplets className="h-5 w-5 text-brand-600" />
            <h2 className="font-semibold text-gray-900">{periodLabel} Quota</h2>
          </div>
          <QuotaGauge used={used} total={limit} />
          <div className="w-full mt-4 grid grid-cols-2 gap-3 text-center">
            <div className="bg-green-50 rounded-lg py-2">
              <p className="text-lg font-bold text-green-700">{formatLitres(limit - used)}</p>
              <p className="text-xs text-gray-500">Remaining</p>
            </div>
            <div className="bg-orange-50 rounded-lg py-2">
              <p className="text-lg font-bold text-orange-700">{formatLitres(used)}</p>
              <p className="text-xs text-gray-500">Used</p>
            </div>
          </div>
          {quota && (
            <p className="text-xs text-gray-400 mt-3">
              Resets: {formatDateTime(quota.resetTimestamp)}
            </p>
          )}
          {quota && <StatusBadge status={quota.status} className="mt-2" />}
        </div>

        {/* QR Code preview */}
        <div className="md:col-span-1 card flex flex-col items-center">
          <div className="flex items-center gap-2 mb-4 self-start">
            <QrCode className="h-5 w-5 text-brand-600" />
            <h2 className="font-semibold text-gray-900">Your QR Code</h2>
          </div>
          {qrToken ? (
            <div className="p-3 bg-white border border-gray-200 rounded-xl shadow-inner">
              <QRCode value={qrToken} size={130} />
            </div>
          ) : (
            <div className="h-40 flex items-center justify-center text-gray-400 text-sm">
              QR code not available
            </div>
          )}
          <Link to="/qr-code" className="mt-4 btn-secondary w-full text-center text-sm py-2 gap-2">
            <QrCode className="h-4 w-4" /> View Full QR Code
          </Link>
        </div>

        {/* Recent transactions */}
        <div className="md:col-span-1 card">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <History className="h-5 w-5 text-brand-600" />
              <h2 className="font-semibold text-gray-900">Recent Activity</h2>
            </div>
            <Link to="/transactions" className="text-xs text-brand-600 hover:underline flex items-center gap-1">
              All <ArrowRight className="h-3 w-3" />
            </Link>
          </div>
          {transactions.length === 0 ? (
            <p className="text-center text-gray-400 text-sm py-6">No transactions yet</p>
          ) : (
            <div className="space-y-3">
              {transactions.map((tx) => (
                <div key={tx.id} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                  <div>
                    <p className="text-sm font-medium text-gray-800">{tx.stationName}</p>
                    <p className="text-xs text-gray-400">{formatDateTime(tx.transactionTimestamp)}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-bold text-brand-700">−{formatLitres(tx.amountDispensedLiters)}</p>
                    <StatusBadge status={tx.status} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
