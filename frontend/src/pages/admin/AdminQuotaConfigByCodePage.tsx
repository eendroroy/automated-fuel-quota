import { useState, useEffect } from 'react'
import { Plus, Edit2, Trash2, Save, X } from 'lucide-react'
import toast from 'react-hot-toast'
import {
  getAllQuotaConfigsByCode,
  createQuotaConfigByCode,
  updateQuotaConfigByCode,
  deleteQuotaConfigByCode,
} from '@/api/quotaConfigByCodeApi'
import { getRegistrationCodes } from '@/api/referenceDataApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import type {
  QuotaConfigByRegistrationCode,
  QuotaConfigByRegistrationCodeRequest,
  QuotaPeriod,
  RegistrationCode,
} from '@/types'

const QUOTA_PERIODS: QuotaPeriod[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY']

export default function AdminQuotaConfigByCodePage() {
  const [configs, setConfigs] = useState<QuotaConfigByRegistrationCode[]>([])
  const [regCodes, setRegCodes] = useState<RegistrationCode[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<QuotaConfigByRegistrationCodeRequest>({
    registrationCode: '',
    limitLitres: 24,
    quotaPeriod: 'WEEKLY',
    description: '',
  })

  const load = async () => {
    setLoading(true)
    try {
      const [configData, codeData] = await Promise.all([
        getAllQuotaConfigsByCode(),
        getRegistrationCodes(),
      ])
      setConfigs(configData)
      setRegCodes(codeData)
    } catch (err) {
      toast.error('Failed to load configurations')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const handleCreate = async () => {
    if (!form.registrationCode || form.limitLitres <= 0) {
      toast.error('Please fill in all required fields')
      return
    }

    try {
      await createQuotaConfigByCode(form)
      toast.success('Configuration created successfully')
      setCreating(false)
      setForm({
        registrationCode: '',
        limitLitres: 24,
        quotaPeriod: 'WEEKLY',
        description: '',
      })
      load()
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Failed to create configuration'
      toast.error(msg)
    }
  }

  const handleUpdate = async (id: string) => {
    if (form.limitLitres <= 0) {
      toast.error('Limit must be greater than 0')
      return
    }

    try {
      await updateQuotaConfigByCode(id, form)
      toast.success('Configuration updated successfully')
      setEditing(null)
      load()
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Failed to update configuration'
      toast.error(msg)
    }
  }

  const handleDelete = async (id: string, code: string) => {
    if (!confirm(`Delete quota configuration for ${code}?`)) return

    try {
      await deleteQuotaConfigByCode(id)
      toast.success('Configuration deleted successfully')
      load()
    } catch (err) {
      toast.error('Failed to delete configuration')
    }
  }

  const startEdit = (config: QuotaConfigByRegistrationCode) => {
    setEditing(config.id)
    setForm({
      registrationCode: config.registrationCode,
      limitLitres: config.limitLitres,
      quotaPeriod: config.quotaPeriod,
      description: config.description || '',
    })
  }

  const cancelEdit = () => {
    setEditing(null)
    setForm({
      registrationCode: '',
      limitLitres: 24,
      quotaPeriod: 'WEEKLY',
      description: '',
    })
  }

  const getAvailableCodes = () => {
    const usedCodes = configs.map((c) => c.registrationCode)
    return regCodes.filter((rc) => !usedCodes.includes(rc.code) || rc.code === form.registrationCode)
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <LoadingSpinner />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quota Configuration by Registration Code</h1>
          <p className="text-sm text-gray-600 mt-1">
            Set different quota limits and periods for different vehicle categories
          </p>
        </div>
        {!creating && (
          <button onClick={() => setCreating(true)} className="btn-primary gap-2">
            <Plus className="h-4 w-4" />
            Add Configuration
          </button>
        )}
      </div>

      {/* Create Form */}
      {creating && (
        <div className="card">
          <h3 className="font-semibold text-gray-900 mb-4">Create New Configuration</h3>
          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <label className="label">Registration Code *</label>
              <select
                className="input-field"
                value={form.registrationCode}
                onChange={(e) => setForm({ ...form, registrationCode: e.target.value })}
              >
                <option value="">Select a code</option>
                {getAvailableCodes().map((rc) => (
                  <option key={rc.code} value={rc.code}>
                    {rc.code} - {rc.description}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Quota Period *</label>
              <select
                className="input-field"
                value={form.quotaPeriod}
                onChange={(e) => setForm({ ...form, quotaPeriod: e.target.value as QuotaPeriod })}
              >
                {QUOTA_PERIODS.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Limit (Litres) *</label>
              <input
                type="number"
                className="input-field"
                min="0.1"
                step="0.1"
                value={form.limitLitres}
                onChange={(e) => setForm({ ...form, limitLitres: parseFloat(e.target.value) || 0 })}
              />
            </div>
            <div>
              <label className="label">Description (optional)</label>
              <input
                className="input-field"
                placeholder="e.g., Daily quota for light automobiles"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
          </div>
          <div className="flex gap-2 mt-4">
            <button onClick={handleCreate} className="btn-primary gap-2">
              <Save className="h-4 w-4" />
              Save Configuration
            </button>
            <button
              onClick={() => {
                setCreating(false)
                cancelEdit()
              }}
              className="btn-secondary gap-2"
            >
              <X className="h-4 w-4" />
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Configurations Table */}
      <div className="card overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Code</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Limit</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Period</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Notes</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {configs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-gray-500">
                    No configurations yet. Click "Add Configuration" to create one.
                  </td>
                </tr>
              ) : (
                configs.map((config) => (
                  <tr key={config.id}>
                    {editing === config.id ? (
                      <>
                        <td className="px-6 py-4 whitespace-nowrap font-mono font-bold">{config.registrationCode}</td>
                        <td className="px-6 py-4 text-sm text-gray-600">{config.registrationCodeDescription}</td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <input
                            type="number"
                            className="input-field w-24"
                            min="0.1"
                            step="0.1"
                            value={form.limitLitres}
                            onChange={(e) => setForm({ ...form, limitLitres: parseFloat(e.target.value) || 0 })}
                          />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <select
                            className="input-field"
                            value={form.quotaPeriod}
                            onChange={(e) => setForm({ ...form, quotaPeriod: e.target.value as QuotaPeriod })}
                          >
                            {QUOTA_PERIODS.map((p) => (
                              <option key={p} value={p}>
                                {p}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td className="px-6 py-4">
                          <input
                            className="input-field"
                            value={form.description}
                            onChange={(e) => setForm({ ...form, description: e.target.value })}
                          />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right">
                          <button onClick={() => handleUpdate(config.id)} className="text-green-600 hover:text-green-700 mr-3">
                            <Save className="h-4 w-4" />
                          </button>
                          <button onClick={cancelEdit} className="text-gray-600 hover:text-gray-700">
                            <X className="h-4 w-4" />
                          </button>
                        </td>
                      </>
                    ) : (
                      <>
                        <td className="px-6 py-4 whitespace-nowrap font-mono font-bold text-brand-600">{config.registrationCode}</td>
                        <td className="px-6 py-4 text-sm text-gray-600">{config.registrationCodeDescription || '-'}</td>
                        <td className="px-6 py-4 whitespace-nowrap font-semibold">{config.limitLitres} L</td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="px-2 py-1 text-xs font-medium bg-blue-100 text-blue-700 rounded">
                            {config.quotaPeriod}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-600">{config.description || '-'}</td>
                        <td className="px-6 py-4 whitespace-nowrap text-right space-x-2">
                          <button onClick={() => startEdit(config)} className="text-blue-600 hover:text-blue-700">
                            <Edit2 className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => handleDelete(config.id, config.registrationCode)}
                            className="text-red-600 hover:text-red-700"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </td>
                      </>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <p className="text-sm text-blue-700">
          <strong>Note:</strong> These configurations apply to new vehicles only. Existing quotas are not affected when you update or delete configurations.
        </p>
      </div>
    </div>
  )
}

