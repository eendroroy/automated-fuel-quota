import { useEffect, useState, useCallback } from 'react'
import { CheckCircle, XCircle, ChevronDown } from 'lucide-react'
import { getAllClaims, approveClaim, rejectClaim } from '@/api/vehicleClaimApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Pagination from '@/components/common/Pagination'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import { formatDateTime } from '@/utils/formatters'
import type { VehicleClaim } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/config/constants'

const STATUS_OPTIONS = [
  { value: '', label: 'All Claims' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
]

export default function AdminVehicleClaimsPage() {
  const [claims, setClaims] = useState<VehicleClaim[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading] = useState(true)

  const [actionModal, setActionModal] = useState<{
    type: 'approve' | 'reject'
    claim: VehicleClaim
  } | null>(null)
  const [adminNotes, setAdminNotes] = useState('')
  const [actionLoading, setActionLoading] = useState(false)

  const fetchClaims = useCallback(() => {
    setLoading(true)
    getAllClaims({ page, size: DEFAULT_PAGE_SIZE, status: statusFilter || undefined })
      .then((d) => {
        setClaims(d.content)
        setTotalPages(d.totalPages)
        setTotalElements(d.totalElements)
      })
      .catch(() => toast.error('Failed to load vehicle claims'))
      .finally(() => setLoading(false))
  }, [page, statusFilter])

  useEffect(() => { fetchClaims() }, [fetchClaims])

  const handleAction = async () => {
    if (!actionModal) return
    if (actionModal.type === 'reject' && !adminNotes.trim()) {
      toast.error('Please provide a reason for rejection')
      return
    }
    setActionLoading(true)
    try {
      if (actionModal.type === 'approve') {
        await approveClaim(actionModal.claim.id, adminNotes || undefined)
        toast.success(`Claim approved — vehicle ${actionModal.claim.registrationNumber} transferred`)
      } else {
        await rejectClaim(actionModal.claim.id, adminNotes)
        toast.success('Claim rejected')
      }
      setActionModal(null)
      setAdminNotes('')
      fetchClaims()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? 'Action failed')
    } finally {
      setActionLoading(false)
    }
  }

  const openModal = (type: 'approve' | 'reject', claim: VehicleClaim) => {
    setAdminNotes('')
    setActionModal({ type, claim })
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Vehicle Ownership Claims</h1>
        <p className="text-sm text-gray-500 mt-0.5">
          {totalElements.toLocaleString()} claim{totalElements !== 1 ? 's' : ''} ·{' '}
          <span className="text-amber-600">
            BRTA verification will be required in a future release
          </span>
        </p>
      </div>

      {/* Filter bar */}
      <div className="card py-4 flex items-center gap-3">
        <div className="relative">
          <select
            className="input-field appearance-none pr-8 text-sm"
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}
          >
            {STATUS_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
          <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Vehicle</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden sm:table-cell">Claimant</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden md:table-cell">Reason</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Status</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden lg:table-cell">Submitted</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={6} className="py-12 text-center"><LoadingSpinner className="mx-auto" /></td></tr>
              ) : claims.length === 0 ? (
                <tr><td colSpan={6} className="py-12 text-center text-gray-400">No claims found</td></tr>
              ) : claims.map((c) => (
                <tr key={c.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3">
                    <p className="font-mono font-semibold text-brand-700">{c.registrationNumber}</p>
                    <p className="text-xs text-gray-400">NID: {c.claimantNid}</p>
                  </td>
                  <td className="px-4 py-3 hidden sm:table-cell">
                    <p className="font-medium text-gray-700">{c.claimantName}</p>
                  </td>
                  <td className="px-4 py-3 hidden md:table-cell text-gray-500 max-w-xs">
                    <p className="truncate" title={c.reason}>{c.reason}</p>
                    {c.adminNotes && (
                      <p className="text-xs text-gray-400 truncate mt-0.5" title={c.adminNotes}>
                        Admin: {c.adminNotes}
                      </p>
                    )}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={c.status} /></td>
                  <td className="px-4 py-3 hidden lg:table-cell text-gray-500 text-xs whitespace-nowrap">
                    {formatDateTime(c.createdAt)}
                  </td>
                  <td className="px-4 py-3">
                    {c.status === 'PENDING' ? (
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          title="Approve claim"
                          onClick={() => openModal('approve', c)}
                          className="p-1.5 text-green-600 hover:bg-green-50 rounded-lg transition-colors"
                        >
                          <CheckCircle className="h-4 w-4" />
                        </button>
                        <button
                          title="Reject claim"
                          onClick={() => openModal('reject', c)}
                          className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                        >
                          <XCircle className="h-4 w-4" />
                        </button>
                      </div>
                    ) : (
                      <span className="text-xs text-gray-400 text-right block pr-1">—</span>
                    )}
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

      {/* Action Modal */}
      <Modal
        isOpen={!!actionModal}
        onClose={() => setActionModal(null)}
        title={actionModal?.type === 'approve' ? 'Approve Ownership Claim' : 'Reject Ownership Claim'}
      >
        {actionModal && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              {actionModal.type === 'approve' ? (
                <>
                  Approve the ownership claim for{' '}
                  <span className="font-semibold font-mono">{actionModal.claim.registrationNumber}</span>?
                  The vehicle will be transferred to{' '}
                  <span className="font-semibold">{actionModal.claim.claimantName}</span> and
                  reset to <strong>PENDING</strong> for re-verification.
                </>
              ) : (
                <>
                  Reject the ownership claim for{' '}
                  <span className="font-semibold font-mono">{actionModal.claim.registrationNumber}</span>?
                  The vehicle will remain with the current owner.
                </>
              )}
            </p>
            <div>
              <label className="label">
                Admin Notes {actionModal.type === 'reject' && <span className="text-red-500">*</span>}
              </label>
              <textarea
                className="input-field resize-none"
                rows={3}
                placeholder={
                  actionModal.type === 'approve'
                    ? 'Optional notes for the record…'
                    : 'Reason for rejection (required)…'
                }
                value={adminNotes}
                onChange={(e) => setAdminNotes(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-3">
              <button onClick={() => setActionModal(null)} className="btn-secondary">Cancel</button>
              {actionModal.type === 'approve' ? (
                <button onClick={handleAction} disabled={actionLoading} className="btn-primary gap-2">
                  {actionLoading && <LoadingSpinner size="sm" />} Approve Transfer
                </button>
              ) : (
                <button onClick={handleAction} disabled={actionLoading} className="btn-danger gap-2">
                  {actionLoading && <LoadingSpinner size="sm" />} Reject Claim
                </button>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

