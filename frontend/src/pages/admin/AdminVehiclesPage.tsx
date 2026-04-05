import { useEffect, useState, useCallback } from 'react'
import { Search, RefreshCw, ChevronDown } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { getAllVehicles, reverifyVehicle } from '@/api/vehicleApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Pagination from '@/components/common/Pagination'
import { formatDate } from '@/utils/formatters'
import toast from 'react-hot-toast'
import type { Vehicle } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'

export default function AdminVehiclesPage() {
  const { t } = useTranslation()

  const STATUS_OPTIONS = [
    { value: '', label: t('adminVehicles.allStatuses') },
    { value: 'VERIFIED', label: t('status.VERIFIED') },
    { value: 'UNVERIFIED', label: t('status.UNVERIFIED') },
    { value: 'DEREGISTERED', label: t('status.DEREGISTERED') },
  ]
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [reverifyingId, setReverifyingId] = useState<string | null>(null)

  const fetchVehicles = useCallback(() => {
    setLoading(true)
    getAllVehicles({ page, size: DEFAULT_PAGE_SIZE, search, status: statusFilter })
      .then((d) => { setVehicles(d.content); setTotalPages(d.totalPages); setTotalElements(d.totalElements) })
      .catch(() => toast.error(t('errors.loadFailed')))
      .finally(() => setLoading(false))
  }, [page, search, statusFilter])

  useEffect(() => { fetchVehicles() }, [fetchVehicles])

  const handleReverify = async (v: Vehicle) => {
    setReverifyingId(v.id)
    try {
      await reverifyVehicle(v.id)
      toast.success(`${v.registrationNumber} ${t('adminVehicles.reverifySuccess')}`)
      fetchVehicles()
    } catch {
      toast.error(t('errors.saveFailed'))
    } finally {
      setReverifyingId(null)
    }
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t('adminVehicles.title')}</h1>
        <p className="text-sm text-gray-500 mt-0.5">{totalElements.toLocaleString()} {t('common.totalItems', { count: totalElements }).replace(/\d+ /, '')}</p>
      </div>

      {/* Filters */}
      <div className="card py-4 flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input className="input-field pl-9" placeholder={t('adminVehicles.searchPlaceholder')}
            value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
        </div>
        <div className="relative">
          <select className="input-field pr-8 appearance-none min-w-[160px]"
            value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}>
            {STATUS_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
          <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
        </div>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-4 py-3 font-semibold text-gray-600">{t('vehicles.registrationNumber')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">{t('adminVehicles.ownerName')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden sm:table-cell">{t('vehicles.vehicleMake')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden md:table-cell">{t('adminVehicles.vehicleClass')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden md:table-cell">{t('adminVehicles.registrationDate')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">{t('adminVehicles.vehicleStatus')}</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={7} className="py-12 text-center"><LoadingSpinner className="mx-auto" /></td></tr>
              ) : vehicles.length === 0 ? (
                <tr><td colSpan={7} className="py-12 text-center text-gray-400">{t('adminVehicles.noVehicles')}</td></tr>
              ) : vehicles.map((v) => (
                <tr key={v.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3">
                    <p className="font-mono font-semibold text-brand-700 text-xs">{v.registrationNumber}</p>
                    {v.customQuotaConfig && (
                      <span className="inline-flex items-center mt-1 px-1.5 py-0.5 rounded-full text-xs bg-purple-100 text-purple-700 font-medium">
                        {t('adminVehicles.customQuota')}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <p className="font-medium text-gray-900">{v.ownerName}</p>
                    <p className="text-xs text-gray-400">{v.ownerEmail}</p>
                  </td>
                  <td className="px-4 py-3 hidden sm:table-cell text-gray-600">
                    {v.vehicleMake} · {v.vehicleColor}
                    {v.engineDisplacement ? <span className="ml-1 text-xs text-gray-400">({v.engineDisplacement}cc)</span> : null}
                  </td>
                  <td className="px-4 py-3 hidden md:table-cell text-gray-500 text-xs max-w-[160px] truncate" title={v.vehicleClass}>{v.vehicleClass}</td>
                  <td className="px-4 py-3 hidden md:table-cell text-gray-500">{formatDate(v.registrationDate)}</td>
                  <td className="px-4 py-3"><StatusBadge status={v.status} /></td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-1.5">
                      <button
                        onClick={() => handleReverify(v)}
                        disabled={reverifyingId === v.id}
                        title="Trigger BRTA re-verification"
                        className="flex items-center gap-1.5 text-xs text-blue-600 hover:bg-blue-50 border border-blue-200 rounded-lg px-2.5 py-1.5 transition-colors disabled:opacity-50"
                      >
                        {reverifyingId === v.id
                          ? <LoadingSpinner size="sm" />
                         : <RefreshCw className="h-3.5 w-3.5" />}
                         {t('adminVehicles.reverify')}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="px-4 pb-4">
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </div>
      </div>
    </div>
  )
}
