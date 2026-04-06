import { useEffect, useState } from 'react'
import { Search, Filter, ChevronDown, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { getAuditLogs } from '@/api/auditApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import Pagination from '@/components/common/Pagination'
import { formatDateTime } from '@/utils/formatters'
import type { AuditLog } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'

const ACTION_OPTIONS = [
  { value: '', label: 'All Actions' },
  { value: 'QUOTA_ADJUSTMENT', label: 'Quota Adjustment' },
  { value: 'QUOTA_RESET', label: 'Quota Reset' },
  { value: 'VEHICLE_APPROVED', label: 'Vehicle Approved' },
  { value: 'VEHICLE_REJECTED', label: 'Vehicle Rejected' },
  { value: 'VEHICLE_SUSPENDED', label: 'Vehicle Suspended' },
  { value: 'STATION_CREATED', label: 'Station Created' },
  { value: 'STATION_UPDATED', label: 'Station Updated' },
  { value: 'USER_SUSPENDED', label: 'User Suspended' },
  { value: 'USER_ACTIVATED', label: 'User Activated' },
]

export default function AdminAuditLogsPage() {
  const { t } = useTranslation()
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [actionTypeFilter, setActionTypeFilter] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [adminSearch, setAdminSearch] = useState('')
  const [targetEntity, setTargetEntity] = useState('')
  const [loading, setLoading] = useState(true)

  const fetchLogs = () => {
    setLoading(true)
    const params: any = { page, size: DEFAULT_PAGE_SIZE }
    if (actionTypeFilter) params.actionType = actionTypeFilter
    if (startDate) params.startDate = startDate + 'T00:00:00'
    if (endDate) params.endDate = endDate + 'T23:59:59'
    if (adminSearch) params.adminSearch = adminSearch
    if (targetEntity) params.targetEntity = targetEntity

    getAuditLogs(params)
      .then((d) => { setLogs(d.content); setTotalPages(d.totalPages); setTotalElements(d.totalElements) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchLogs() }, [page, actionTypeFilter, startDate, endDate, adminSearch, targetEntity])

  const formatActionType = (action: string) =>
    action.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ')

  const getActionColor = (action: string) => {
    if (action.includes('APPROVED') || action.includes('CREATED') || action.includes('ACTIVATED')) return 'text-green-600 dark:text-green-400'
    if (action.includes('REJECTED') || action.includes('SUSPENDED') || action.includes('DELETED') || action.includes('DEACTIVATED')) return 'text-red-600 dark:text-red-400'
    if (action.includes('UPDATED') || action.includes('ADJUSTMENT') || action.includes('SYNC')) return 'text-blue-600 dark:text-blue-400'
    return 'text-gray-600 dark:text-gray-400'
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('adminAuditLogs.title')}</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{totalElements.toLocaleString()} {t('adminAuditLogs.auditRecords')}</p>
      </div>

      {/* Filters */}
      <div className="card py-4 space-y-3">
        {/* Row 1: admin search + target entity */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <input className="input-field pl-9 text-sm" placeholder={t('adminAuditLogs.adminSearchPlaceholder')}
              value={adminSearch} onChange={(e) => { setAdminSearch(e.target.value); setPage(0) }} />
            {adminSearch && (
              <button onClick={() => { setAdminSearch(''); setPage(0) }} className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-gray-100 rounded">
                <X className="h-3 w-3 text-gray-400" />
              </button>
            )}
          </div>
          <div className="relative flex-1">
            <input className="input-field text-sm" placeholder={t('adminAuditLogs.targetEntityPlaceholder')}
              value={targetEntity} onChange={(e) => { setTargetEntity(e.target.value); setPage(0) }} />
            {targetEntity && (
              <button onClick={() => { setTargetEntity(''); setPage(0) }} className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-gray-100 rounded">
                <X className="h-3 w-3 text-gray-400" />
              </button>
            )}
          </div>
        </div>

        {/* Row 2: action type + date range */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative min-w-[200px]">
            <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">{t('adminAuditLogs.filterAction')}</label>
            <div className="relative">
              <select className="input-field pr-8 appearance-none"
                value={actionTypeFilter} onChange={(e) => { setActionTypeFilter(e.target.value); setPage(0) }}>
                <option value="">{t('adminAuditLogs.allActions')}</option>
                <option value="QUOTA_ADJUSTMENT">{t('adminAuditLogs.quotaAdjustment')}</option>
                <option value="QUOTA_RESET">{t('adminAuditLogs.quotaReset')}</option>
                <option value="VEHICLE_APPROVED">{t('adminAuditLogs.vehicleApproved')}</option>
                <option value="VEHICLE_REJECTED">{t('adminAuditLogs.vehicleRejected')}</option>
                <option value="VEHICLE_SUSPENDED">{t('adminAuditLogs.vehicleSuspended')}</option>
                <option value="STATION_CREATED">{t('adminAuditLogs.stationCreated')}</option>
                <option value="STATION_UPDATED">{t('adminAuditLogs.stationUpdated')}</option>
                <option value="USER_SUSPENDED">{t('adminAuditLogs.userSuspended')}</option>
                <option value="USER_ACTIVATED">{t('adminAuditLogs.userActivated')}</option>
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>
          <div>
            <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">{t('adminAuditLogs.startDate')}</label>
            <input className="input-field" type="date" value={startDate} onChange={(e) => { setStartDate(e.target.value); setPage(0) }} />
          </div>
          <div>
            <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">{t('adminAuditLogs.endDate')}</label>
            <input className="input-field" type="date" value={endDate} onChange={(e) => { setEndDate(e.target.value); setPage(0) }} />
          </div>
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 dark:bg-gray-800/60 border-b border-gray-100 dark:border-gray-700">
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">{t('adminAuditLogs.timestamp')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">{t('adminAuditLogs.admin')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400">{t('adminAuditLogs.action')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400 hidden lg:table-cell">{t('adminAuditLogs.target')}</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 dark:text-gray-400 hidden xl:table-cell">{t('adminAuditLogs.reason')}</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={5} className="py-12 text-center"><LoadingSpinner className="mx-auto" /></td></tr>
              ) : logs.length === 0 ? (
                <tr><td colSpan={5} className="py-12 text-center text-gray-400">{t('adminAuditLogs.noLogs')}</td></tr>
              ) : logs.map((log) => (
                <tr key={log.id} className="border-b border-gray-50 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors">
                  <td className="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs">{formatDateTime(log.actionTimestamp)}</td>
                  <td className="px-4 py-3">
                    <p className="font-medium text-gray-800 dark:text-gray-200">{log.adminName}</p>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`font-medium ${getActionColor(log.actionType)}`}>
                      {formatActionType(log.actionType)}
                    </span>
                  </td>
                  <td className="px-4 py-3 hidden lg:table-cell text-gray-600 dark:text-gray-400">
                    {log.targetEntity}
                    {log.targetEntityId && <span className="text-xs text-gray-400 dark:text-gray-500 block">ID: {log.targetEntityId.substring(0, 8)}</span>}
                  </td>
                  <td className="px-4 py-3 hidden xl:table-cell text-gray-500 dark:text-gray-400 text-xs max-w-xs truncate">
                    {log.reasonNotes ?? '-'}
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
