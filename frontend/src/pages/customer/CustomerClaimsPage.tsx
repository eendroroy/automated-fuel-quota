import { useEffect, useState, useCallback } from 'react'
import { Plus, FileText, AlertCircle } from 'lucide-react'
import { getMyClaims, claimVehicle } from '@/api/vehicleClaimApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import { formatDateTime } from '@/utils/formatters'
import type { VehicleClaim, ClaimVehicleRequest } from '@/types'

const empty: ClaimVehicleRequest = {
  registrationNumber: '',
  claimantNid: '',
  reason: '',
}

export default function CustomerClaimsPage() {
  const [claims, setClaims] = useState<VehicleClaim[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<ClaimVehicleRequest>(empty)
  const [submitting, setSubmitting] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    getMyClaims()
      .then(setClaims)
      .catch(() => toast.error('Failed to load claims'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const set = (k: keyof ClaimVehicleRequest) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm((f) => ({ ...f, [k]: e.target.value }))

  const handleSubmit = async () => {
    if (!form.registrationNumber.trim() || !form.claimantNid.trim() || !form.reason.trim()) {
      toast.error('Please fill in all required fields')
      return
    }
    setSubmitting(true)
    try {
      await claimVehicle(form)
      toast.success('Ownership claim submitted! An admin will review it shortly.')
      setModalOpen(false)
      setForm(empty)
      load()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? 'Failed to submit claim')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <LoadingSpinner size="lg" />
    </div>
  )

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Vehicle Ownership Claims</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Claim a vehicle that was previously registered under another account
          </p>
        </div>
        <button onClick={() => { setForm(empty); setModalOpen(true) }} className="btn-primary gap-2">
          <Plus className="h-4 w-4" /> New Claim
        </button>
      </div>

      {/* Info box */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl px-4 py-3 flex gap-3 text-sm text-blue-700">
        <AlertCircle className="h-5 w-5 flex-shrink-0 mt-0.5" />
        <div>
          <p className="font-medium">Second-hand vehicle purchase?</p>
          <p className="text-blue-600 mt-0.5">
            If you purchased a vehicle that is already registered in this system, submit a claim here.
            An admin will verify your ownership and transfer the vehicle to your account.
            Future releases will integrate BRTA for automatic verification.
          </p>
        </div>
      </div>

      {/* Claims list */}
      {claims.length === 0 ? (
        <div className="card text-center py-16 text-gray-400">
          <FileText className="h-12 w-12 mx-auto mb-3 opacity-30" />
          <p className="font-medium">No ownership claims yet</p>
          <p className="text-sm mt-1">Click "New Claim" to request a vehicle ownership transfer.</p>
        </div>
      ) : (
        <div className="card p-0 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Vehicle</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden sm:table-cell">Reason</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">Status</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 hidden md:table-cell">Submitted</th>
              </tr>
            </thead>
            <tbody>
              {claims.map((c) => (
                <tr key={c.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3">
                    <p className="font-mono font-semibold text-brand-700">{c.registrationNumber}</p>
                    <p className="text-xs text-gray-400">NID: {c.claimantNid}</p>
                  </td>
                  <td className="px-4 py-3 hidden sm:table-cell text-gray-600 max-w-xs">
                    <p className="truncate" title={c.reason}>{c.reason}</p>
                    {c.adminNotes && (
                      <p className="text-xs text-gray-400 mt-0.5 truncate" title={c.adminNotes}>
                        Admin: {c.adminNotes}
                      </p>
                    )}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={c.status} /></td>
                  <td className="px-4 py-3 hidden md:table-cell text-gray-500 text-xs">
                    {formatDateTime(c.createdAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Submit Claim Modal */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title="Submit Ownership Claim">
        <div className="space-y-4">
          <p className="text-sm text-gray-500">
            Provide the registration number of the vehicle you own and your NID as proof of ownership.
            An admin will review and transfer the vehicle to your account.
          </p>
          <div>
            <label className="label">Registration Number *</label>
            <input
              className="input-field font-mono"
              placeholder="e.g. DHK-CA-1234"
              value={form.registrationNumber}
              onChange={set('registrationNumber')}
            />
          </div>
          <div>
            <label className="label">Your NID (Proof of Ownership) *</label>
            <input
              className="input-field"
              placeholder="e.g. 199012345678"
              value={form.claimantNid}
              onChange={set('claimantNid')}
            />
          </div>
          <div>
            <label className="label">Reason / Context *</label>
            <textarea
              className="input-field resize-none"
              rows={3}
              placeholder="e.g. Purchased this vehicle second-hand on 01-Jan-2026 from previous owner"
              value={form.reason}
              onChange={set('reason')}
            />
          </div>
          <div className="flex justify-end gap-3 pt-1">
            <button onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button>
            <button onClick={handleSubmit} disabled={submitting} className="btn-primary gap-2">
              {submitting ? <LoadingSpinner size="sm" /> : <Plus className="h-4 w-4" />}
              Submit Claim
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
