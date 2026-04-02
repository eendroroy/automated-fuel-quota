import { useEffect, useState } from 'react'
import { Save, Settings2, RefreshCw } from 'lucide-react'
import { getQuotaConfig, updateQuotaConfig } from '@/api/quotaConfigApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import toast from 'react-hot-toast'
import type { QuotaConfig, QuotaConfigUpdateRequest, QuotaPeriod } from '@/types'
import { QUOTA_PERIODS } from '@/config/constants'

const CRON_PRESETS: { label: string; period: QuotaPeriod; cron: string }[] = [
  { label: 'Daily (midnight)', period: 'DAILY',     cron: '0 0 0 * * ?' },
  { label: 'Weekly (Sunday midnight)', period: 'WEEKLY',    cron: '0 0 0 ? * SUN' },
  { label: 'Monthly (1st of month)', period: 'MONTHLY',   cron: '0 0 0 1 * ?' },
  { label: 'Quarterly (1st of quarter)', period: 'QUARTERLY', cron: '0 0 0 1 1,4,7,10 ?' },
  { label: 'Yearly (1 Jan)', period: 'YEARLY',    cron: '0 0 0 1 1 ?' },
]

export default function AdminQuotaConfigPage() {
  const [config, setConfig] = useState<QuotaConfig | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const [form, setForm] = useState<QuotaConfigUpdateRequest>({
    limitLitres: 24,
    geofenceRadiusMeters: 100,
    quotaPeriod: 'WEEKLY',
    resetCronExpression: '0 0 0 ? * SUN',
    description: '',
  })

  const load = () => {
    setLoading(true)
    getQuotaConfig()
      .then((cfg) => {
        setConfig(cfg)
        setForm({
          limitLitres: Number(cfg.limitLitres),
          geofenceRadiusMeters: cfg.geofenceRadiusMeters,
          quotaPeriod: cfg.quotaPeriod,
          resetCronExpression: cfg.resetCronExpression,
          description: cfg.description ?? '',
        })
      })
      .catch(() => toast.error('Failed to load quota configuration'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const applyPreset = (period: QuotaPeriod) => {
    const preset = CRON_PRESETS.find((p) => p.period === period)
    if (preset) {
      setForm((f) => ({ ...f, quotaPeriod: period, resetCronExpression: preset.cron }))
    } else {
      setForm((f) => ({ ...f, quotaPeriod: period }))
    }
  }

  const handleSave = async () => {
    if (form.limitLitres <= 0) { toast.error('Limit must be greater than 0'); return }
    if (form.geofenceRadiusMeters < 10) { toast.error('Geofence radius must be at least 10m'); return }
    if (!form.resetCronExpression.trim()) { toast.error('Cron expression is required'); return }

    setSaving(true)
    try {
      const updated = await updateQuotaConfig(form)
      setConfig(updated)
      toast.success('Quota configuration updated successfully')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? 'Failed to update configuration')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <LoadingSpinner size="lg" />
    </div>
  )

  return (
    <div className="space-y-6 max-w-2xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quota Configuration</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Global settings applied to all newly created vehicle quotas
          </p>
        </div>
        <button onClick={load} className="btn-secondary gap-2 text-sm">
          <RefreshCw className="h-4 w-4" /> Refresh
        </button>
      </div>

      {config && (
        <div className="bg-brand-50 border border-brand-200 rounded-xl px-4 py-3 text-sm text-brand-700">
          <strong>Current:</strong> {Number(config.limitLitres).toFixed(1)} L per{' '}
          <span className="font-semibold">{config.quotaPeriod.toLowerCase()}</span> period
          {config.updatedAt && (
            <span className="text-brand-500 ml-2">
              · Last updated {new Date(config.updatedAt).toLocaleDateString()}
            </span>
          )}
        </div>
      )}

      <div className="card space-y-5">
        <div className="flex items-center gap-2 pb-2 border-b border-gray-100">
          <Settings2 className="h-5 w-5 text-brand-600" />
          <h2 className="font-semibold text-gray-800">Edit Configuration</h2>
        </div>

        {/* Fuel limit */}
        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="label">Fuel Limit (Litres) *</label>
            <input
              type="number"
              step="0.5"
              min="1"
              max="1000"
              className="input-field"
              value={form.limitLitres}
              onChange={(e) => setForm((f) => ({ ...f, limitLitres: parseFloat(e.target.value) || 0 }))}
            />
            <p className="text-xs text-gray-400 mt-1">Maximum fuel allocated per quota period</p>
          </div>
          <div>
            <label className="label">Geofence Radius (Metres) *</label>
            <input
              type="number"
              step="10"
              min="10"
              max="10000"
              className="input-field"
              value={form.geofenceRadiusMeters}
              onChange={(e) => setForm((f) => ({ ...f, geofenceRadiusMeters: parseInt(e.target.value) || 100 }))}
            />
            <p className="text-xs text-gray-400 mt-1">Max distance from pump station allowed</p>
          </div>
        </div>

        {/* Quota period */}
        <div>
          <label className="label">Quota Reset Period *</label>
          <div className="grid grid-cols-3 sm:grid-cols-5 gap-2 mt-1">
            {QUOTA_PERIODS.map((p) => (
              <button
                key={p}
                type="button"
                onClick={() => applyPreset(p)}
                className={`px-3 py-2 rounded-lg text-sm font-medium border transition-colors ${
                  form.quotaPeriod === p
                    ? 'bg-brand-600 text-white border-brand-600'
                    : 'bg-white text-gray-600 border-gray-300 hover:border-brand-400 hover:text-brand-600'
                }`}
              >
                {p.charAt(0) + p.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
          <p className="text-xs text-gray-400 mt-2">
            Selecting a period auto-fills the recommended cron expression below
          </p>
        </div>

        {/* Cron expression */}
        <div>
          <label className="label">Reset Cron Expression *</label>
          <input
            type="text"
            className="input-field font-mono text-sm"
            placeholder="0 0 0 ? * SUN"
            value={form.resetCronExpression}
            onChange={(e) => setForm((f) => ({ ...f, resetCronExpression: e.target.value }))}
          />
          <p className="text-xs text-gray-400 mt-1">
            Spring cron format (6 fields): seconds minutes hours day-of-month month day-of-week
          </p>
          <div className="mt-2 grid sm:grid-cols-2 gap-1.5">
            {CRON_PRESETS.map((p) => (
              <button
                key={p.cron}
                type="button"
                onClick={() => setForm((f) => ({ ...f, quotaPeriod: p.period, resetCronExpression: p.cron }))}
                className="text-left text-xs text-gray-500 hover:text-brand-600 hover:bg-brand-50 rounded px-2 py-1 transition-colors"
              >
                <span className="font-mono text-gray-400">{p.cron}</span>
                <span className="ml-2 text-gray-600">{p.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Description */}
        <div>
          <label className="label">Change Notes <span className="text-gray-400 font-normal">(optional)</span></label>
          <textarea
            className="input-field resize-none"
            rows={3}
            placeholder="Reason for this configuration change…"
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
          />
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-2 border-t border-gray-100">
          <button onClick={load} className="btn-secondary">Reset</button>
          <button onClick={handleSave} disabled={saving} className="btn-primary gap-2">
            {saving ? <LoadingSpinner size="sm" /> : <Save className="h-4 w-4" />}
            Save Configuration
          </button>
        </div>
      </div>

      <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 text-sm text-amber-700">
        <strong>Note:</strong> Changes to the quota limit and period only apply to <em>newly created</em> quotas.
        Existing vehicle quotas are not automatically updated. Use the Quota Management page to adjust individual quotas.
      </div>
    </div>
  )
}

