import { useEffect, useState, useCallback } from 'react'
import { Save, Settings2, RefreshCw, Plus, Edit2, Trash2, X, RefreshCcw, Tags } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  getQuotaConfig, updateQuotaConfig,
  getAllQuotaConfigSets, createQuotaConfigSet, updateQuotaConfigSet, deleteQuotaConfigSet,
  syncQuotaConfigs,
} from '@/api/quotaConfigApi'
import { getRegistrationCodes } from '@/api/referenceDataApi'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import type { QuotaConfig, QuotaConfigUpdateRequest, QuotaPeriod, QuotaConfigSet, QuotaConfigSetRequest, RegistrationCode } from '@/types'
import { QUOTA_PERIODS } from '@/config/constants'

const CRON_PRESETS: { label: string; period: QuotaPeriod; cron: string }[] = [
  { label: 'Daily (midnight)', period: 'DAILY',     cron: '0 0 0 * * ?' },
  { label: 'Weekly (Sunday midnight)', period: 'WEEKLY',    cron: '0 0 0 ? * SUN' },
  { label: 'Monthly (1st of month)', period: 'MONTHLY',   cron: '0 0 0 1 * ?' },
  { label: 'Quarterly (1st of quarter)', period: 'QUARTERLY', cron: '0 0 0 1 1,4,7,10 ?' },
  { label: 'Yearly (1 Jan)', period: 'YEARLY',    cron: '0 0 0 1 1 ?' },
]

const emptySetForm = (): QuotaConfigSetRequest => ({
  name: '',
  limitLitres: 24,
  quotaPeriod: 'WEEKLY',
  description: '',
  registrationCodes: [],
})

export default function AdminQuotaConfigPage() {
  const { t } = useTranslation()

  // ── Global config state ───────────────────────────────────────────────────
  const [config, setConfig] = useState<QuotaConfig | null>(null)
  const [configLoading, setConfigLoading] = useState(true)
  const [configSaving, setConfigSaving] = useState(false)
  const [form, setForm] = useState<QuotaConfigUpdateRequest>({
    limitLitres: 24,
    geofenceRadiusMeters: 100,
    quotaPeriod: 'WEEKLY',
    resetCronExpression: '0 0 0 ? * SUN',
    description: '',
  })

  // ── Config sets state ─────────────────────────────────────────────────────
  const [sets, setSets] = useState<QuotaConfigSet[]>([])
  const [setsLoading, setSetsLoading] = useState(true)
  const [regCodes, setRegCodes] = useState<RegistrationCode[]>([])
  const [setModal, setSetModal] = useState<{ open: boolean; editing: QuotaConfigSet | null }>({ open: false, editing: null })
  const [configSetForm, setConfigSetForm] = useState<QuotaConfigSetRequest>(emptySetForm())
  const [setFormSaving, setSetFormSaving] = useState(false)

  // ── Sync state ────────────────────────────────────────────────────────────
  const [syncing, setSyncing] = useState(false)

  const loadConfig = useCallback(() => {
    setConfigLoading(true)
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
      .catch(() => toast.error(t('errors.loadFailed')))
      .finally(() => setConfigLoading(false))
  }, [t])

  const loadSets = useCallback(() => {
    setSetsLoading(true)
    Promise.all([getAllQuotaConfigSets(), getRegistrationCodes()])
      .then(([setsData, codesData]) => { setSets(setsData); setRegCodes(codesData) })
      .catch(() => toast.error(t('errors.loadFailed')))
      .finally(() => setSetsLoading(false))
  }, [t])

  useEffect(() => { loadConfig(); loadSets() }, [loadConfig, loadSets])

  // ── Global config handlers ────────────────────────────────────────────────
  const applyPreset = (period: QuotaPeriod) => {
    const preset = CRON_PRESETS.find((p) => p.period === period)
    setForm((f) => preset
      ? { ...f, quotaPeriod: period, resetCronExpression: preset.cron }
      : { ...f, quotaPeriod: period })
  }

  const handleSaveConfig = async () => {
    if (form.limitLitres <= 0) { toast.error('Limit must be greater than 0'); return }
    if (form.geofenceRadiusMeters < 10) { toast.error('Geofence radius must be at least 10m'); return }
    if (!form.resetCronExpression.trim()) { toast.error('Cron expression is required'); return }
    setConfigSaving(true)
    try {
      const updated = await updateQuotaConfig(form)
      setConfig(updated)
      toast.success(t('adminQuotaConfig.configUpdated'))
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? t('errors.saveFailed'))
    } finally {
      setConfigSaving(false)
    }
  }

  // ── Config set handlers ───────────────────────────────────────────────────
  const openCreate = () => { setConfigSetForm(emptySetForm()); setSetModal({ open: true, editing: null }) }
  const openEdit = (s: QuotaConfigSet) => {
    setConfigSetForm({ name: s.name, limitLitres: Number(s.limitLitres), quotaPeriod: s.quotaPeriod, description: s.description ?? '', registrationCodes: [...s.registrationCodes] })
    setSetModal({ open: true, editing: s })
  }

  const handleSaveSet = async () => {
    if (!configSetForm.name.trim()) { toast.error(t('adminQuotaConfigSets.nameRequired')); return }
    if (configSetForm.limitLitres <= 0) { toast.error('Limit must be greater than 0'); return }
    if (configSetForm.registrationCodes.length === 0) { toast.error(t('adminQuotaConfigSets.codesRequired')); return }

    setSetFormSaving(true)
    try {
      if (setModal.editing) {
        await updateQuotaConfigSet(setModal.editing.id, configSetForm)
        toast.success(t('adminQuotaConfigSets.setUpdated'))
      } else {
        await createQuotaConfigSet(configSetForm)
        toast.success(t('adminQuotaConfigSets.setCreated'))
      }
      setSetModal({ open: false, editing: null })
      loadSets()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? t('errors.saveFailed'))
    } finally {
      setSetFormSaving(false)
    }
  }

  const handleDeleteSet = async (s: QuotaConfigSet) => {
    if (!confirm(t('adminQuotaConfigSets.confirmDelete', { name: s.name }))) return
    try {
      await deleteQuotaConfigSet(s.id)
      toast.success(t('adminQuotaConfigSets.setDeleted'))
      loadSets()
    } catch { toast.error(t('errors.deleteFailed')) }
  }

  const handleSync = async () => {
    if (!confirm(t('adminQuotaConfigSets.confirmSync'))) return
    setSyncing(true)
    try {
      const result = await syncQuotaConfigs()
      toast.success(t('adminQuotaConfigSets.syncSuccess', { count: result.updatedCount }))
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? t('errors.saveFailed'))
    } finally {
      setSyncing(false)
    }
  }

  // Codes already claimed by other sets (for conflict prevention in the form)
  const usedCodes = sets
    .filter((s) => !setModal.editing || s.id !== setModal.editing.id)
    .flatMap((s) => s.registrationCodes)

  const toggleCode = (code: string) => {
    setConfigSetForm((f) => ({
      ...f,
      registrationCodes: f.registrationCodes.includes(code)
        ? f.registrationCodes.filter((c) => c !== code)
        : [...f.registrationCodes, code],
    }))
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('adminQuotaConfig.title')}</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{t('adminQuotaConfig.subtitle')}</p>
        </div>
        <button
          onClick={handleSync}
          disabled={syncing}
          className="btn-primary gap-2 bg-emerald-600 hover:bg-emerald-700 border-emerald-600"
        >
          {syncing ? <LoadingSpinner size="sm" /> : <RefreshCcw className="h-4 w-4" />}
          {t('adminQuotaConfigSets.syncButton')}
        </button>
      </div>

      {/* ── Config Sets Section ─────────────────────────────────────────────── */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Tags className="h-5 w-5 text-brand-600" />
            <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-200">{t('adminQuotaConfigSets.title')}</h2>
            <span className="text-xs text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded-full">{sets.length}</span>
          </div>
          <div className="flex gap-2">
            <button onClick={loadSets} className="btn-secondary gap-2 text-sm">
              <RefreshCw className="h-4 w-4" /> {t('common.refresh')}
            </button>
            <button onClick={openCreate} className="btn-primary gap-2 text-sm">
              <Plus className="h-4 w-4" /> {t('adminQuotaConfigSets.addSet')}
            </button>
          </div>
        </div>

        <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg px-4 py-3 text-sm text-blue-700 dark:text-blue-300">
          {t('adminQuotaConfigSets.setsNote')}
        </div>

        <div className="card p-0 overflow-hidden">
          {setsLoading ? (
            <div className="flex items-center justify-center h-32"><LoadingSpinner /></div>
          ) : sets.length === 0 ? (
            <div className="text-center py-12 text-gray-400">
              <Tags className="h-10 w-10 mx-auto mb-3 opacity-30" />
              <p>{t('adminQuotaConfigSets.noSets')}</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-100 dark:divide-gray-800">
              {sets.map((s) => (
                <div key={s.id} className="flex items-start gap-4 px-5 py-4 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="font-semibold text-gray-900 dark:text-white">{s.name}</p>
                      <span className="text-xs font-medium bg-brand-100 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300 px-2 py-0.5 rounded-full">
                        {Number(s.limitLitres).toFixed(1)} L / {t(`adminQuotaConfig.${s.quotaPeriod}`)}
                      </span>
                    </div>
                    {s.description && (
                      <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5">{s.description}</p>
                    )}
                    <div className="flex flex-wrap gap-1.5 mt-2">
                      {s.registrationCodes.map((code) => {
                        const detail = s.registrationCodeDetails?.find((d) => d.code === code)
                        return (
                          <span
                            key={code}
                            title={detail?.description}
                            className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-mono font-bold bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 border border-gray-200 dark:border-gray-600"
                          >
                            {code}
                          </span>
                        )
                      })}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 flex-shrink-0">
                    <button onClick={() => openEdit(s)} className="p-1.5 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/30 rounded-lg transition-colors">
                      <Edit2 className="h-4 w-4" />
                    </button>
                    <button onClick={() => handleDeleteSet(s)} className="p-1.5 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors">
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* ── Global Config Section ──────────────────────────────────────────── */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Settings2 className="h-5 w-5 text-gray-500 dark:text-gray-400" />
            <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-200">{t('adminQuotaConfig.defaultConfig')}</h2>
          </div>
          <button onClick={loadConfig} className="btn-secondary gap-2 text-sm">
            <RefreshCw className="h-4 w-4" /> {t('common.refresh')}
          </button>
        </div>

        {config && (
          <div className="bg-brand-50 dark:bg-brand-900/20 border border-brand-200 dark:border-brand-800 rounded-xl px-4 py-3 text-sm text-brand-700 dark:text-brand-300">
            <strong>{t('adminQuotaConfig.current')}:</strong> {Number(config.limitLitres).toFixed(1)} L {t('adminQuotaConfig.perPeriod')}{' '}
            <span className="font-semibold">{t(`adminQuotaConfig.${config.quotaPeriod}`)}</span> {t('adminQuotaConfig.period')}
            {config.updatedAt && (
              <span className="text-brand-500 ml-2">
                · {t('adminQuotaConfig.lastUpdated')} {new Date(config.updatedAt).toLocaleDateString()}
              </span>
            )}
          </div>
        )}

        {configLoading ? (
          <div className="flex items-center justify-center h-32"><LoadingSpinner /></div>
        ) : (
          <div className="card space-y-5">
            {/* Fuel limit + geofence */}
            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="label">{t('adminQuotaConfig.fuelLimit')} *</label>
                <input type="number" step="0.5" min="1" max="1000" className="input-field"
                  value={form.limitLitres}
                  onChange={(e) => setForm((f) => ({ ...f, limitLitres: parseFloat(e.target.value) || 0 }))} />
                <p className="text-xs text-gray-400 mt-1">{t('adminQuotaConfig.fuelLimitDesc')}</p>
              </div>
              <div>
                <label className="label">{t('adminQuotaConfig.geofenceRadius')} *</label>
                <input type="number" step="10" min="10" max="10000" className="input-field"
                  value={form.geofenceRadiusMeters}
                  onChange={(e) => setForm((f) => ({ ...f, geofenceRadiusMeters: parseInt(e.target.value) || 100 }))} />
                <p className="text-xs text-gray-400 mt-1">{t('adminQuotaConfig.geofenceDesc')}</p>
              </div>
            </div>

            {/* Quota period */}
            <div>
              <label className="label">{t('adminQuotaConfig.quotaResetPeriod')} *</label>
              <div className="grid grid-cols-3 sm:grid-cols-5 gap-2 mt-1">
                {QUOTA_PERIODS.map((p) => (
                  <button key={p} type="button" onClick={() => applyPreset(p)}
                    className={`px-3 py-2 rounded-lg text-sm font-medium border transition-colors ${
                      form.quotaPeriod === p
                        ? 'bg-brand-600 text-white border-brand-600'
                        : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:border-brand-400 hover:text-brand-600'
                    }`}>
                    {t(`adminQuotaConfig.${p}`)}
                  </button>
                ))}
              </div>
              <p className="text-xs text-gray-400 mt-2">{t('adminQuotaConfig.periodDesc')}</p>
            </div>

            {/* Cron */}
            <div>
              <label className="label">{t('adminQuotaConfig.resetCron')} *</label>
              <input type="text" className="input-field font-mono text-sm" placeholder="0 0 0 ? * SUN"
                value={form.resetCronExpression}
                onChange={(e) => setForm((f) => ({ ...f, resetCronExpression: e.target.value }))} />
              <p className="text-xs text-gray-400 mt-1">{t('adminQuotaConfig.cronFormat')}</p>
              <div className="mt-2 grid sm:grid-cols-2 gap-1.5">
                {CRON_PRESETS.map((p) => (
                  <button key={p.cron} type="button"
                    onClick={() => setForm((f) => ({ ...f, quotaPeriod: p.period, resetCronExpression: p.cron }))}
                    className="text-left text-xs text-gray-500 dark:text-gray-400 hover:text-brand-600 hover:bg-brand-50 dark:hover:bg-brand-900/20 rounded px-2 py-1 transition-colors">
                    <span className="font-mono text-gray-400 dark:text-gray-500">{p.cron}</span>
                    <span className="ml-2 text-gray-600 dark:text-gray-400">{t(`adminQuotaConfig.preset_${p.label.replace(/\s+/g, '_').toLowerCase()}`)}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Description */}
            <div>
              <label className="label">{t('adminQuotaConfig.changeNotes')} <span className="text-gray-400 font-normal">({t('common.optional')})</span></label>
              <textarea className="input-field resize-none" rows={3}
                placeholder={t('adminQuotaConfig.changeNotesPlaceholder')}
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
            </div>

            <div className="flex justify-end gap-3 pt-2 border-t border-gray-100 dark:border-gray-700">
              <button onClick={loadConfig} className="btn-secondary">{t('common.reset')}</button>
              <button onClick={handleSaveConfig} disabled={configSaving} className="btn-primary gap-2">
                {configSaving ? <LoadingSpinner size="sm" /> : <Save className="h-4 w-4" />}
                {t('adminQuotaConfig.updateConfig')}
              </button>
            </div>
          </div>
        )}

        <div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-xl px-4 py-3 text-sm text-amber-700 dark:text-amber-300">
          <strong>{t('common.note')}:</strong> {t('adminQuotaConfig.changeNote')}
        </div>
      </div>

      {/* ── Config Set Modal ───────────────────────────────────────────────── */}
      <Modal
        isOpen={setModal.open}
        onClose={() => setSetModal({ open: false, editing: null })}
        title={setModal.editing ? t('adminQuotaConfigSets.editSet') : t('adminQuotaConfigSets.createSet')}
      >
        <div className="space-y-4">
          <div className="grid sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="label">{t('adminQuotaConfigSets.setName')} *</label>
              <input className="input-field" placeholder={t('adminQuotaConfigSets.setNamePlaceholder')}
                value={configSetForm.name}
                onChange={(e) => setConfigSetForm((f) => ({ ...f, name: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t('adminQuotaConfigSets.limitLitres')} *</label>
              <input type="number" step="0.5" min="1" max="1000" className="input-field"
                value={configSetForm.limitLitres}
                onChange={(e) => setConfigSetForm((f) => ({ ...f, limitLitres: parseFloat(e.target.value) || 0 }))} />
            </div>
            <div>
              <label className="label">{t('adminQuotaConfigSets.period')} *</label>
              <select className="input-field" value={configSetForm.quotaPeriod}
                onChange={(e) => setConfigSetForm((f) => ({ ...f, quotaPeriod: e.target.value as QuotaPeriod }))}>
                {QUOTA_PERIODS.map((p) => (
                  <option key={p} value={p}>{t(`adminQuotaConfig.${p}`)}</option>
                ))}
              </select>
            </div>
            <div className="sm:col-span-2">
              <label className="label">{t('adminQuotaConfigSets.description')} ({t('common.optional')})</label>
              <input className="input-field" placeholder={t('adminQuotaConfigSets.descriptionPlaceholder')}
                value={configSetForm.description}
                onChange={(e) => setConfigSetForm((f) => ({ ...f, description: e.target.value }))} />
            </div>
          </div>

          <div>
            <label className="label">{t('adminQuotaConfigSets.registrationCodes')} *</label>
            <p className="text-xs text-gray-400 mb-2">{t('adminQuotaConfigSets.codesHint')}</p>
            <div className="flex flex-wrap gap-2 p-3 border border-gray-200 dark:border-gray-700 rounded-lg max-h-48 overflow-y-auto">
              {regCodes.map((rc) => {
                const isSelected = configSetForm.registrationCodes.includes(rc.code)
                const isUsedByOther = usedCodes.includes(rc.code)
                return (
                  <button
                    key={rc.code}
                    type="button"
                    disabled={isUsedByOther}
                    title={isUsedByOther ? t('adminQuotaConfigSets.codeTaken') : rc.description}
                    onClick={() => toggleCode(rc.code)}
                    className={`px-2.5 py-1 rounded-md text-xs font-mono font-bold border transition-colors ${
                      isSelected
                        ? 'bg-brand-600 text-white border-brand-600'
                        : isUsedByOther
                          ? 'bg-gray-100 dark:bg-gray-700 text-gray-300 dark:text-gray-500 border-gray-200 dark:border-gray-600 cursor-not-allowed'
                          : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:border-brand-400 hover:text-brand-600'
                    }`}
                  >
                    {rc.code}
                  </button>
                )
              })}
            </div>
            {configSetForm.registrationCodes.length > 0 && (
              <p className="text-xs text-brand-600 mt-1">
                {t('adminQuotaConfigSets.selectedCodes', { count: configSetForm.registrationCodes.length })}: {configSetForm.registrationCodes.join(', ')}
              </p>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-2 border-t border-gray-100 dark:border-gray-700">
            <button onClick={() => setSetModal({ open: false, editing: null })} className="btn-secondary gap-2">
              <X className="h-4 w-4" /> {t('common.cancel')}
            </button>
            <button onClick={handleSaveSet} disabled={setFormSaving} className="btn-primary gap-2">
              {setFormSaving ? <LoadingSpinner size="sm" /> : <Save className="h-4 w-4" />}
              {setModal.editing ? t('adminQuotaConfigSets.updateSet') : t('adminQuotaConfigSets.createSet')}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

