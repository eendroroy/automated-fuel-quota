import { useEffect, useState, useCallback, useMemo, memo } from 'react'
import { Search, SlidersHorizontal, RotateCcw, Filter, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { getAllQuotas, adjustQuota, manualResetQuota } from '@/api/quotaApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Pagination from '@/components/common/Pagination'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import { formatDateTime, formatLitres } from '@/utils/formatters'
import type { Quota } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'

const AdminQuotasPage = memo(function AdminQuotasPage() {
  const { t } = useTranslation()
  const [quotas, setQuotas] = useState<Quota[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [sortBy, setSortBy] = useState<'registration' | 'usage' | 'remaining' | 'resetDate'>('registration')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc')
  const [loading, setLoading] = useState(true)
  const [adjustModal, setAdjustModal] = useState<Quota | null>(null)
  const [newLimit, setNewLimit] = useState('')
  const [reasonText, setReasonText] = useState('')
  const [saving, setSaving] = useState(false)
  const [showFilters, setShowFilters] = useState(false)

  // Debounce search to avoid too many API calls
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 300)
    return () => clearTimeout(timer)
  }, [search])

  const fetchQuotas = useCallback(() => {
    setLoading(true)
    getAllQuotas({
      page,
      size: DEFAULT_PAGE_SIZE,
      search: debouncedSearch,
      status: statusFilter || undefined,
      sortBy: sortBy !== 'registration' ? sortBy : undefined,
      sortOrder
    })
      .then((d) => {
        setQuotas(d.content);
        setTotalPages(d.totalPages);
        setTotalElements(d.totalElements)
      })
      .catch(() => toast.error(t('adminQuotas.loadFailed')))
      .finally(() => setLoading(false))
  }, [page, debouncedSearch, statusFilter, sortBy, sortOrder])

  useEffect(() => {
    fetchQuotas()
  }, [fetchQuotas])

  // Memoized filtered and sorted quotas for performance
  const processedQuotas = useMemo(() => {
    let result = [...quotas]

    // Apply client-side sorting if needed
    if (sortBy === 'usage') {
      result.sort((a, b) => {
        const aUsage = (a.usedLiters / a.limitLiters) * 100
        const bUsage = (b.usedLiters / b.limitLiters) * 100
        return sortOrder === 'asc' ? aUsage - bUsage : bUsage - aUsage
      })
    } else if (sortBy === 'remaining') {
      result.sort((a, b) => {
        return sortOrder === 'asc'
          ? a.remainingLiters - b.remainingLiters
          : b.remainingLiters - a.remainingLiters
      })
    }

    return result
  }, [quotas, sortBy, sortOrder])

  const handleAdjust = async () => {
    if (!adjustModal || !newLimit || !reasonText.trim()) {
      toast.error(t('adminQuotas.fillRequiredFields'));
      return
    }

    const limitValue = parseFloat(newLimit)
    if (isNaN(limitValue) || limitValue <= 0 || limitValue > 500) {
      toast.error(t('adminQuotas.validLimitError'))
      return
    }

    setSaving(true)
    try {
      await adjustQuota(adjustModal.vehicleId, { newLimitLiters: limitValue, reason: reasonText })
      toast.success(t('adminQuotas.quotaAdjusted', { limit: formatLitres(limitValue) }))
      setAdjustModal(null)
      setNewLimit('')
      setReasonText('')
      fetchQuotas()
    } catch (err: any) {
      const message = err?.response?.data?.message || t('adminQuotas.adjustFailed')
      toast.error(message)
    } finally {
      setSaving(false)
    }
  }

  const handleReset = async (q: Quota) => {
    if (!confirm(t('adminQuotas.confirmReset', { registrationNumber: q.registrationNumber }))) return
    try {
      await manualResetQuota(q.vehicleId);
      toast.success(t('adminQuotas.resetSuccess'));
      fetchQuotas()
    }
    catch {
      toast.error(t('errors.saveFailed'))
    }
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('adminQuotas.title')}</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{totalElements.toLocaleString()} {t('common.totalItems', { count: totalElements }).replace(/\d+ /, '')}</p>
      </div>

      {/* Enhanced Search and Filters */}
      <div className="card py-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <input
              className="input-field pl-9"
              placeholder={t('adminQuotas.searchPlaceholder') || 'Search by registration number or owner name...'}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0) }}
            />
            {search && (
              <button
                onClick={() => { setSearch(''); setPage(0) }}
                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-gray-100 rounded"
              >
                <X className="h-3 w-3 text-gray-400" />
              </button>
            )}
          </div>

          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-lg border transition-colors ${
              showFilters || statusFilter
                ? 'bg-brand-50 text-brand-700 border-brand-200'
                : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700'
            }`}
          >
            <Filter className="h-4 w-4" />
            {t('common.filter')}
          </button>
        </div>

        {showFilters && (
          <div className="mt-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700">
            <div className="grid sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">{t('common.status')}</label>
                <select
                  className="input-field text-sm"
                  value={statusFilter}
                  onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}
                >
                  <option value="">{t('adminQuotas.allStatuses')}</option>
                  <option value="ACTIVE">{t('status.ACTIVE')}</option>
                  <option value="SUSPENDED">{t('status.SUSPENDED')}</option>
                  <option value="EXHAUSTED">{t('adminQuotas.exhausted')}</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">{t('adminQuotas.sortBy')}</label>
                <select
                  className="input-field text-sm"
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value as any)}
                >
                  <option value="registration">Registration</option>
                  <option value="usage">Usage %</option>
                  <option value="remaining">Remaining</option>
                  <option value="resetDate">Reset Date</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Order</label>
                <select
                  className="input-field text-sm"
                  value={sortOrder}
                  onChange={(e) => setSortOrder(e.target.value as any)}
                >
                  <option value="asc">Ascending</option>
                  <option value="desc">Descending</option>
                </select>
              </div>
            </div>
          </div>
        )}
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 dark:bg-gray-800/60 border-b border-gray-100 dark:border-gray-700">
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">Vehicle</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400 hidden sm:table-cell">Limit / Period</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">Used / Remaining</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400 hidden md:table-cell">Reset At</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">Status</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} className="py-12 text-center"><LoadingSpinner className="mx-auto" /></td></tr>
              ) : processedQuotas.length === 0 ? (
                <tr><td colSpan={6} className="py-12 text-center text-gray-400">
                  {search || statusFilter ? 'No quotas match your search criteria' : 'No quotas found'}
                </td></tr>
              ) : processedQuotas.map((q) => {
                const pct = Math.round((q.usedLiters / q.limitLiters) * 100)
                const isLowQuota = pct >= 90
                const isMediumQuota = pct >= 60 && pct < 90

                return (
                  <tr key={q.id} className={`border-b border-gray-50 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors ${
                    isLowQuota ? 'bg-red-50/30 dark:bg-red-900/10' : ''
                  }`}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2 flex-wrap">
                        <p className="font-mono font-semibold text-brand-700">{q.registrationNumber}</p>
                        {isLowQuota && (
                          <span className="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300">
                            Low
                          </span>
                        )}
                        {q.individuallyOverridden && (
                          <span className="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300">
                            Custom
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-gray-400">{q.ownerName}</p>
                    </td>
                    <td className="px-4 py-3 hidden sm:table-cell">
                      <p className="text-gray-600 dark:text-gray-400">{formatLitres(q.limitLiters)}</p>
                      <span className="text-xs text-brand-600 bg-brand-50 dark:bg-brand-900/20 px-1.5 py-0.5 rounded font-medium">
                        {q.period ?? 'WEEKLY'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 bg-gray-100 dark:bg-gray-700 rounded-full h-1.5 max-w-[80px]">
                          <div
                            className={`h-1.5 rounded-full transition-all ${
                              isLowQuota ? 'bg-red-500' : isMediumQuota ? 'bg-yellow-400' : 'bg-green-500'
                            }`}
                            style={{ width: `${Math.min(pct, 100)}%` }}
                          />
                        </div>
                        <span className={`text-xs font-medium ${
                          isLowQuota ? 'text-red-600' : isMediumQuota ? 'text-yellow-600' : 'text-gray-500 dark:text-gray-400'
                        }`}>
                          {pct}%
                        </span>
                      </div>
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                        {formatLitres(q.usedLiters)} used · {formatLitres(q.remainingLiters)} left
                      </p>
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell text-gray-500 dark:text-gray-400 text-xs">
                      {formatDateTime(q.resetTimestamp)}
                    </td>
                    <td className="px-4 py-3"><StatusBadge status={q.status} /></td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          title="Adjust Quota"
                          onClick={() => {
                            setAdjustModal(q);
                            setNewLimit(String(q.limitLiters));
                            setReasonText('')
                          }}
                          className="p-1.5 text-brand-600 hover:bg-brand-50 dark:hover:bg-brand-900/20 rounded-lg transition-colors"
                        >
                          <SlidersHorizontal className="h-4 w-4" />
                        </button>
                        <button
                          title="Reset Quota"
                          onClick={() => handleReset(q)}
                          className="p-1.5 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                        >
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
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Adjusting quota for <span className="font-semibold">{adjustModal?.registrationNumber}</span>
            {adjustModal?.period && (
              <span className="ml-2 text-xs text-brand-600 bg-brand-50 dark:bg-brand-900/20 px-1.5 py-0.5 rounded">
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
})

export default AdminQuotasPage
