import { useEffect, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { getBrtaOffices, getRegistrationCodes } from '@/api/referenceDataApi'
import type { BrtaOffice, RegistrationCode } from '@/types'

interface RegistrationNumberValue {
  brtaOfficeCode: string
  vehicleRegistrationCode: string
  serialPart1: string
  serialPart2: string
}

interface Props {
  value: RegistrationNumberValue
  onChange: (val: RegistrationNumberValue) => void
  disabled?: boolean
}

/**
 * Structured vehicle registration number input.
 *
 * Renders four parts:
 *   [BRTA dropdown] [CODE dropdown] [2-digit text] - [4-digit text]
 *
 * Example output: DHAKA METRO GA 11-1234
 */
export default function RegistrationNumberInput({ value, onChange, disabled }: Props) {
  const [brtaOffices, setBrtaOffices] = useState<BrtaOffice[]>([])
  const [regCodes, setRegCodes] = useState<RegistrationCode[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getBrtaOffices(), getRegistrationCodes()])
      .then(([offices, codes]) => {
        setBrtaOffices(offices)
        setRegCodes(codes)
        // Set defaults if not yet set — use a single onChange call so both
        // fields are applied together and neither overwrites the other.
        const newVal = { ...value }
        if (!value.brtaOfficeCode && offices.length > 0) {
          newVal.brtaOfficeCode = offices[0].brtaCode
        }
        if (!value.vehicleRegistrationCode && codes.length > 0) {
          newVal.vehicleRegistrationCode = codes[0].code
        }
        onChange(newVal)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const set = (field: keyof RegistrationNumberValue) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      onChange({ ...value, [field]: e.target.value })

  if (loading) {
    return (
      <div className="h-10 bg-gray-100 animate-pulse rounded-lg" />
    )
  }

  return (
    <div className="flex flex-wrap gap-1.5 items-center">
      {/* BRTA Office dropdown */}
      <div className="relative flex-[3] min-w-[150px]">
        <select
          className="input-field appearance-none pr-8 text-sm"
          value={value.brtaOfficeCode}
          onChange={set('brtaOfficeCode')}
          disabled={disabled}
          title="Select BRTA Office"
        >
          {brtaOffices.map((o) => (
            <option key={o.brtaCode} value={o.brtaCode}>
              {o.brtaCode}
            </option>
          ))}
        </select>
        <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
      </div>

      {/* Vehicle category code dropdown */}
      <div className="relative flex-[2] min-w-[90px]">
        <select
          className="input-field appearance-none pr-8 text-sm"
          value={value.vehicleRegistrationCode}
          onChange={set('vehicleRegistrationCode')}
          disabled={disabled}
          title="Select vehicle registration code"
        >
          {regCodes.map((c) => (
            <option key={c.code} value={c.code} title={c.description}>
              {c.code}
            </option>
          ))}
        </select>
        <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
      </div>

      {/* 2-digit serial */}
      <input
        className="input-field flex-1 min-w-[60px] max-w-[70px] text-sm text-center font-mono"
        placeholder="11"
        maxLength={2}
        value={value.serialPart1}
        onChange={(e) => {
          const val = e.target.value.replace(/\D/g, '').slice(0, 2)
          onChange({ ...value, serialPart1: val })
        }}
        disabled={disabled}
        title="2-digit serial number"
      />

      <span className="text-gray-500 font-bold select-none">-</span>

      {/* 4-digit serial */}
      <input
        className="input-field flex-1 min-w-[70px] max-w-[90px] text-sm text-center font-mono"
        placeholder="1234"
        maxLength={4}
        value={value.serialPart2}
        onChange={(e) => {
          const val = e.target.value.replace(/\D/g, '').slice(0, 4)
          onChange({ ...value, serialPart2: val })
        }}
        disabled={disabled}
        title="4-digit serial number"
      />

      {/* Preview */}
      {value.brtaOfficeCode && value.vehicleRegistrationCode && value.serialPart1 && value.serialPart2 && (
        <p className="w-full text-xs text-gray-500 mt-0.5 font-mono">
          {value.brtaOfficeCode} {value.vehicleRegistrationCode} {value.serialPart1}-{value.serialPart2}
        </p>
      )}
    </div>
  )
}

