import { useEffect, useState, useCallback } from 'react'
import { Search, SlidersHorizontal, RotateCcw } from 'lucide-react'
import { getAllQuotas, adjustQuota, manualResetQuota } from '@/api/quotaApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Pagination from '@/components/common/Pagination'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import { formatDateTime, formatLitres } from '@/utils/formatters'
import type { Quota } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'

export default function AdminQuotasPage() {
  const [quotas, setQuotas] = useState<Quota[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [adjustModal, setAdjustModal] = useState<Quota | null>(null)
  const [newLimit, setNewLimit] = useState('')
  const [reasonText, setReasonText] = useState('')
  const [saving, setSaving] = useState(false)

  const fetchQuotas = useCallback(() => {
    setLoading(true)
    getAllQuotas({ page, size: DEFAULT_PAGE_SIZE, search })
      .then((d) => { setQuotas(d.content); setTotalPages(d.totalPages); setTotalElements(d.totalElements) })
      .catch(() => toast.error('Failed to load quotas'))
      .finally(() => setLoading(false))
  }, [page, search])

  useEffect(() => { fetchQuotas() }, [fetchQuotas])

  const handleAdjust = async () => {
    if (!adjustModal || !newLimit || !reasonText.trim()) { toast.error('Fill all fields'); return }
    setSaving(true)
    try {
      await adjustQuota(adjustModal.vehicleId, { newLimitLiters: parseFloat(newLimit), reason: reasonText })
      toast.success('Quota adjusted')
      setAdjustModal(null); fetchQuotas()
    } catch { toast.error('Failed to adjust quota') }
    finally { setSaving(false) }
  }

  const handleReset = async (q: Quota) => {
    if (!confirm(`Reset quota for ${q.registrationNumber}?`)) return
    try { await manualResetQuota(q.vehicleId); toast.success('Quota reset'); fetchQuotas() }
    catch { toast.error('Reset failed') }
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Quota Management</h1>
        <p className="text-sm text-gray-500 mt-0.5">{totalElements.toLocaleString()} vehicles</p>
      </div>

      {/* Search */}
      <div className="card py-4">
        <div className="relative max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input className="input-field pl-9" placeholder="Search by registration number…"
            value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Vehicle</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden sm:table-cell">Limit / Period</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Used / Remaining</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden md:table-cell">Reset At</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Status</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} className="py-12 text-center"><LoadingSpinner className="mx-auto" /></td></tr>
              ) : quotas.length === 0 ? (
                <tr><td colSpan={6} className="py-12 text-center text-gray-400">No quotas found</td></tr>
              ) : quotas.map((q) => {
                const pct = Math.round((q.usedLiters / q.limitLiters) * 100)
                return (
                  <tr key={q.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <p className="font-mono font-semibold text-brand-700">{q.registrationNumber}</p>
                      <p className="text-xs text-gray-400">{q.ownerName}</p>
                    </td>
                    <td className="px-4 py-3 hidden sm:table-cell">
                      <p className="text-gray-600">{formatLitres(q.limitLiters)}</p>
                      <span className="text-xs text-brand-600 bg-brand-50 px-1.5 py-0.5 rounded font-medium">
                        {q.period ?? 'WEEKLY'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 bg-gray-100 rounded-full h-1.5 max-w-[80px]">
                          <div className={`h-1.5 rounded-full ${pct >= 90 ? 'bg-red-500' : pct >= 60 ? 'bg-yellow-400' : 'bg-green-500'}`}
                            style={{ width: `${Math.min(pct, 100)}%` }} />
                        </div>
                        <span className="text-xs text-gray-500">{pct}%</span>
                      </div>
                      <p className="text-xs text-gray-500 mt-0.5">{formatLitres(q.usedLiters)} used · {formatLitres(q.remainingLiters)} left</p>
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell text-gray-500 text-xs">{formatDateTime(q.resetTimestamp)}</td>
                    <td className="px-4 py-3"><StatusBadge status={q.status} /></td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1.5">
                        <button title="Adjust Quota" onClick={() => { setAdjustModal(q); setNewLimit(String(q.limitLiters)); setReasonText('') }}
                          className="p-1.5 text-brand-600 hover:bg-brand-50 rounded-lg transition-colors">
                          <SlidersHorizontal className="h-4 w-4" />
                        </button>
                        <button title="Reset Quota" onClick={() => handleReset(q)}
                          className="p-1.5 text-gray-500 hover:bg-gray-100 rounded-lg transition-colors">
                          <RotateCcw className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
        <div className="px-4 pb-4"><Pagination page={page} totalPages={totalPages} onPageChange={setPage} /></div>
      </div>

      {/* Adjust Modal */}
      <Modal isOpen={!!adjustModal} onClose={() => setAdjustModal(null)} title="Adjust Quota">
        <div className="space-y-4">
          <p className="text-sm text-gray-600">
            Adjusting quota for <span className="font-semibold">{adjustModal?.registrationNumber}</span>
            {adjustModal?.period && (
              <span className="ml-2 text-xs text-brand-600 bg-brand-50 px-1.5 py-0.5 rounded">
                {adjustModal.period} period
              </span>
            )}
          </p>
          <div>
            <label className="label">New Limit (Litres per {adjustModal?.period?.toLowerCase() ?? 'period'})</label>
            <input type="number" step="0.5" min="1" max="500" className="input-field"
              value={newLimit} onChange={(e) => setNewLimit(e.target.value)} />
          </div>
          <div>
            <label className="label">Reason *</label>
            <textarea className="input-field resize-none" rows={3}
              placeholder="Reason for quota adjustment…"
              value={reasonText} onChange={(e) => setReasonText(e.target.value)} />
          </div>
          <div className="flex justify-end gap-3">
            <button onClick={() => setAdjustModal(null)} className="btn-secondary">Cancel</button>
            <button onClick={handleAdjust} disabled={saving} className="btn-primary gap-2">
              {saving && <LoadingSpinner size="sm" />} Apply Adjustment
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
