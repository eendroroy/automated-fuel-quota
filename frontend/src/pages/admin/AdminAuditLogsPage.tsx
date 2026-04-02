import { useEffect, useState } from 'react'
import { Search, Filter, ChevronDown } from 'lucide-react'
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
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [actionTypeFilter, setActionTypeFilter] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [loading, setLoading] = useState(true)

  const fetchLogs = () => {
    setLoading(true)
    const params: any = { page, size: DEFAULT_PAGE_SIZE }
    if (actionTypeFilter) params.actionType = actionTypeFilter
    if (startDate) params.startDate = startDate + 'T00:00:00'
    if (endDate) params.endDate = endDate + 'T23:59:59'

    getAuditLogs(params)
      .then((d) => { setLogs(d.content); setTotalPages(d.totalPages); setTotalElements(d.totalElements) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchLogs() }, [page, actionTypeFilter, startDate, endDate])

  const formatActionType = (action: string) =>
    action.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ')

  const getActionColor = (action: string) => {
    if (action.includes('APPROVED') || action.includes('CREATED')) return 'text-green-600'
    if (action.includes('REJECTED') || action.includes('SUSPENDED') || action.includes('DELETED')) return 'text-red-600'
    if (action.includes('UPDATED') || action.includes('ADJUSTMENT')) return 'text-blue-600'
    return 'text-gray-600'
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Audit Logs</h1>
        <p className="text-sm text-gray-500 mt-0.5">{totalElements.toLocaleString()} audit records</p>
      </div>

      {/* Filters */}
      <div className="card py-4 flex flex-col md:flex-row gap-3">
        <div className="flex-1">
          <label className="block text-xs text-gray-500 mb-1">Action Type</label>
          <div className="relative">
            <select className="input-field pr-8 appearance-none"
              value={actionTypeFilter} onChange={(e) => { setActionTypeFilter(e.target.value); setPage(0) }}>
              {ACTION_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          </div>
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Start Date</label>
          <input className="input-field" type="date" value={startDate} onChange={(e) => { setStartDate(e.target.value); setPage(0) }} />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">End Date</label>
          <input className="input-field" type="date" value={endDate} onChange={(e) => { setEndDate(e.target.value); setPage(0) }} />
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Timestamp</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Admin</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Action</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden lg:table-cell">Target Entity</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden xl:table-cell">Reason</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={5} className="py-12 text-center"><LoadingSpinner className="mx-auto" /></td></tr>
              ) : logs.length === 0 ? (
                <tr><td colSpan={5} className="py-12 text-center text-gray-400">No audit logs found</td></tr>
              ) : logs.map((log) => (
                <tr key={log.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 text-gray-500 text-xs">{formatDateTime(log.actionTimestamp)}</td>
                  <td className="px-4 py-3">
                    <p className="font-medium text-gray-800">{log.adminName}</p>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`font-medium ${getActionColor(log.actionType)}`}>
                      {formatActionType(log.actionType)}
                    </span>
                  </td>
                  <td className="px-4 py-3 hidden lg:table-cell text-gray-600">
                    {log.targetEntity}
                    {log.targetEntityId && <span className="text-xs text-gray-400 block">ID: {log.targetEntityId.substring(0, 8)}</span>}
                  </td>
                  <td className="px-4 py-3 hidden xl:table-cell text-gray-500 text-xs max-w-xs truncate">
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
