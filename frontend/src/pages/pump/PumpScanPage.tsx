import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Html5QrcodeScanner, Html5QrcodeScanType } from 'html5-qrcode'
import { MapPin, ScanLine, Loader2, AlertCircle, Keyboard } from 'lucide-react'
import toast from 'react-hot-toast'
import { authorizeDispensing, authorizeByRegistration } from '@/api/pumpApi'
import { getPumpSession } from '@/layouts/PumpRepLayout'
import type { AuthorizationResult } from '@/types'

const SCANNER_ID = 'pump-qr-scanner'
type Mode = 'scan' | 'manual'

export default function PumpScanPage() {
  const navigate = useNavigate()
  const { t } = useTranslation()
  const session = getPumpSession()
  const scannerRef = useRef<Html5QrcodeScanner | null>(null)
  const [mode, setMode] = useState<Mode>('scan')
  const [scanning, setScanning] = useState(false)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [regNumber, setRegNumber] = useState('')
  const [manualLoading, setManualLoading] = useState(false)

  // Redirect if not logged in
  useEffect(() => {
    if (!session) navigate('/pump', { replace: true })
  }, [session, navigate])

  // ── QR Scanner lifecycle ────────────────────────────────────────────────────
  useEffect(() => {
    if (!session || mode !== 'scan') return

    const scanner = new Html5QrcodeScanner(
      SCANNER_ID,
      {
        fps: 10,
        qrbox: { width: 260, height: 260 },
        supportedScanTypes: [Html5QrcodeScanType.SCAN_TYPE_CAMERA],
        rememberLastUsedCamera: true,
        showTorchButtonIfSupported: true,
      },
      false
    )

    scanner.render(
      async (decodedText) => {
        if (processing) return
        setProcessing(true)
        setError(null)
        scanner.pause(true)

        try {
          const auth: AuthorizationResult = await authorizeDispensing({
            qrToken: decodedText,
            stationId: session!.stationId,
          })
          navigate('/pump/dispense', {
            state: { auth, qrToken: decodedText, session },
          })
        } catch (err: any) {
          const msg = err?.response?.data?.message ?? err?.response?.data ?? 'Failed to authorize. Try again.'
          const text = typeof msg === 'string' ? msg : 'Authorization failed'
          setError(text)
          toast.error(text)
          scanner.resume()
          setProcessing(false)
        }
      },
      () => {} // ignore per-frame failures
    )

    scannerRef.current = scanner
    setScanning(true)

    return () => {
      scanner.clear().catch(() => {})
      setScanning(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode])

  // ── Manual authorize ────────────────────────────────────────────────────────
  const handleManualSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!regNumber.trim() || !session) return
    setManualLoading(true)
    setError(null)
    try {
      const auth: AuthorizationResult = await authorizeByRegistration({
        registrationNumber: regNumber.trim().toUpperCase(),
        stationId: session.stationId,
      })
      navigate('/pump/dispense', {
        state: { auth, registrationNumber: regNumber.trim().toUpperCase(), session },
      })
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? err?.response?.data ?? 'Vehicle not found or lookup failed.'
      const text = typeof msg === 'string' ? msg : 'Lookup failed'
      setError(text)
      toast.error(text)
    } finally {
      setManualLoading(false)
    }
  }

  if (!session) return null

  return (
    <div className="space-y-4">
      {/* Station info banner */}
      <div className="flex items-center gap-3 bg-brand-50 dark:bg-brand-900/20 border border-brand-200 dark:border-brand-800 rounded-xl px-4 py-3">
        <MapPin className="h-5 w-5 text-brand-600 dark:text-brand-400 shrink-0" />
        <div className="min-w-0">
          <p className="text-xs text-brand-500 dark:text-brand-400 font-medium">{t('pumpScan.loggedInAs')}</p>
          <p className="text-sm font-semibold text-brand-800 dark:text-brand-200 truncate">
            {session.name} · {session.stationName}
          </p>
        </div>
      </div>

      {/* Mode toggle */}
      <div className="flex bg-gray-100 dark:bg-gray-800 rounded-xl p-1 gap-1">
        <button
          onClick={() => { setMode('scan'); setError(null) }}
          className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg text-sm font-medium transition-all ${
            mode === 'scan'
              ? 'bg-white dark:bg-gray-700 text-brand-700 dark:text-brand-300 shadow-sm'
              : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
          }`}
        >
          <ScanLine className="h-4 w-4" />
          {t('pumpScan.scanTab')}
        </button>
        <button
          onClick={() => { setMode('manual'); setError(null) }}
          className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-lg text-sm font-medium transition-all ${
            mode === 'manual'
              ? 'bg-white dark:bg-gray-700 text-brand-700 dark:text-brand-300 shadow-sm'
              : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
          }`}
        >
          <Keyboard className="h-4 w-4" />
          {t('pumpScan.manualTab')}
        </button>
      </div>

      {/* ── QR Scanner pane ──────────────────────────────────────────────────── */}
      {mode === 'scan' && (
        <>
          <div className="text-center">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">{t('pumpScan.scanQrTitle')}</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{t('pumpScan.scanQrDesc')}</p>
          </div>

          <div className="rounded-xl overflow-hidden border-2 border-brand-200 bg-black relative">
            <div id={SCANNER_ID} className="w-full" />
            {processing && (
              <div className="absolute inset-0 bg-black/60 flex flex-col items-center justify-center gap-3">
                <Loader2 className="h-10 w-10 text-white animate-spin" />
                <p className="text-white font-medium text-sm">Verifying QR code…</p>
              </div>
            )}
          </div>

          {scanning && !processing && (
            <p className="text-center text-xs text-gray-400">{t('pumpDispense.waitingForQR')}</p>
          )}
        </>
      )}

      {/* ── Manual entry pane ────────────────────────────────────────────────── */}
      {mode === 'manual' && (
        <>
          <div className="text-center">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">{t('pumpScan.enterManualTitle')}</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{t('pumpScan.enterManualDesc')}</p>
          </div>

          <form onSubmit={handleManualSubmit} className="card space-y-4">
            <div>
              <label className="label" htmlFor="reg-number">{t('pumpScan.manualLabel')}</label>
              <input
                id="reg-number"
                type="text"
                className="input-field text-center text-lg font-mono tracking-widest uppercase"
                placeholder={t('pumpScan.manualPlaceholder')}
                value={regNumber}
                onChange={(e) => setRegNumber(e.target.value)}
                autoFocus
                autoComplete="off"
                autoCapitalize="characters"
              />
            </div>
            <button
              type="submit"
              disabled={!regNumber.trim() || manualLoading}
              className="btn-primary w-full py-3.5 gap-2 text-base"
            >
              {manualLoading ? <Loader2 className="h-5 w-5 animate-spin" /> : <ScanLine className="h-5 w-5" />}
              {manualLoading ? t('pumpScan.lookingUp') : t('pumpScan.verifyBtn')}
            </button>
          </form>
        </>
      )}

      {/* Error message (shared) */}
      {error && (
        <div className="flex items-start gap-2 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl px-4 py-3 text-sm text-red-700 dark:text-red-400">
          <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
          <span>{error}</span>
        </div>
      )}
    </div>
  )
}

