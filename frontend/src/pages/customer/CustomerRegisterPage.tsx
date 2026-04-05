import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Check, ChevronRight, User, Car, FileText, Smartphone } from 'lucide-react'
import toast from 'react-hot-toast'
import { registerCustomer, sendOtp } from '@/api/authApi'
import { FUEL_TYPES } from '@/config/constants'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import RegistrationNumberInput from '@/components/common/RegistrationNumberInput'
import type { RegisterVehicleRequest } from '@/types'

// ── Step indicator ────────────────────────────────────────────────────────────
const steps = [
  { id: 1, label: 'Personal Info', icon: User },
  { id: 2, label: 'Verify Mobile', icon: Smartphone },
  { id: 3, label: 'Vehicle Details (Optional)', icon: Car },
  { id: 4, label: 'Review & Submit', icon: FileText },
]

interface FormData extends RegisterVehicleRequest {
  confirmPassword: string
}

const empty: FormData = {
  ownerName: '',
  ownerNid: '',
  ownerMobile: '',
  ownerEmail: '',
  password: '',
  confirmPassword: '',
  otp: '',
  brtaOfficeCode: '',
  vehicleRegistrationCode: '',
  serialPart1: '',
  serialPart2: '',
  vehicleMake: '',
  vehicleColor: '',
  fuelType: FUEL_TYPES[0],
  engineDisplacement: undefined,
  registrationDate: '',
}

export default function CustomerRegisterPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [form, setForm] = useState<FormData>(empty)
  const [loading, setLoading] = useState(false)
  const [otpSending, setOtpSending] = useState(false)
  const [otpSent, setOtpSent] = useState(false)

  const set = (k: keyof FormData) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  const validateStep1 = () => {
    if (!form.ownerName || !form.ownerNid || !form.ownerMobile || !form.ownerEmail) {
      toast.error('Please fill in all personal details')
      return false
    }
    if (!/^01[3-9]\d{8}$/.test(form.ownerMobile)) {
      toast.error('Please enter a valid mobile number (e.g. 01711123456)')
      return false
    }
    if (!/^\S+@\S+\.\S+$/.test(form.ownerEmail)) {
      toast.error('Please enter a valid email')
      return false
    }
    if (form.password.length < 8) {
      toast.error('Password must be at least 8 characters')
      return false
    }
    if (form.password !== form.confirmPassword) {
      toast.error('Passwords do not match')
      return false
    }
    return true
  }

  const handleSendOtp = async () => {
    setOtpSending(true)
    try {
      await sendOtp(form.ownerMobile)
      setOtpSent(true)
      toast.success(`OTP sent to ${form.ownerMobile}`)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? 'Failed to send OTP. Please try again.')
    } finally {
      setOtpSending(false)
    }
  }

  const validateStep2 = () => {
    if (!otpSent) {
      toast.error('Please request an OTP first')
      return false
    }
    if (!form.otp || !/^\d{6}$/.test(form.otp)) {
      toast.error('Please enter the 6-digit OTP sent to your mobile')
      return false
    }
    return true
  }

  const validateStep3 = () => {
    // Check if user has started entering vehicle info
    const hasAnyVehicleInfo = form.brtaOfficeCode || form.vehicleRegistrationCode ||
                              form.serialPart1 || form.serialPart2 ||
                              form.vehicleMake || form.vehicleColor || form.registrationDate

    // If no vehicle info at all, allow skip (driver-only registration)
    if (!hasAnyVehicleInfo) {
      return true
    }

    // If partial vehicle info, require all fields
    if (!form.brtaOfficeCode || !form.vehicleRegistrationCode) {
      toast.error('Please select BRTA office and registration code, or skip this step')
      return false
    }
    if (!form.serialPart1 || !/^\d{2}$/.test(form.serialPart1)) {
      toast.error('Serial part 1 must be exactly 2 digits')
      return false
    }
    if (!form.serialPart2 || !/^\d{4}$/.test(form.serialPart2)) {
      toast.error('Serial part 2 must be exactly 4 digits')
      return false
    }
    if (!form.vehicleMake || !form.vehicleColor || !form.registrationDate) {
      toast.error('Please fill in all vehicle details')
      return false
    }
    return true
  }

  const hasVehicleInfo = () => {
    return form.brtaOfficeCode && form.vehicleRegistrationCode &&
           form.serialPart1 && form.serialPart2 &&
           form.vehicleMake && form.vehicleColor && form.registrationDate
  }

  const next = async () => {
    if (step === 1) {
      if (!validateStep1()) return
      // Auto-send OTP when moving to step 2
      setStep(2)
      setOtpSending(true)
      try {
        await sendOtp(form.ownerMobile)
        setOtpSent(true)
        toast.success(`OTP sent to ${form.ownerMobile}`)
      } catch (err: unknown) {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        toast.error(msg ?? 'Failed to send OTP. Please try again.')
      } finally {
        setOtpSending(false)
      }
      return
    }
    if (step === 2 && !validateStep2()) return
    if (step === 3 && !validateStep3()) return
    setStep((s) => s + 1)
  }

  const skipVehicle = () => {
    if (step === 3) {
      setStep(4)
    }
  }

  const assembledRegNumber = () => {
    if (!hasVehicleInfo()) return null
    return `${form.brtaOfficeCode ?? ''} ${form.vehicleRegistrationCode ?? ''} ${form.serialPart1 ?? ''}-${form.serialPart2 ?? ''}`
  }

  const handleSubmit = async () => {
    setLoading(true)
    try {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { confirmPassword, ...payload } = form

      // Only include vehicle fields if they're provided
      const requestPayload: RegisterVehicleRequest = {
        ownerName: payload.ownerName,
        ownerNid: payload.ownerNid,
        ownerMobile: payload.ownerMobile,
        ownerEmail: payload.ownerEmail,
        password: payload.password,
        otp: payload.otp,
      }

      if (hasVehicleInfo()) {
        requestPayload.brtaOfficeCode = payload.brtaOfficeCode
        requestPayload.vehicleRegistrationCode = payload.vehicleRegistrationCode
        requestPayload.serialPart1 = payload.serialPart1
        requestPayload.serialPart2 = payload.serialPart2
        requestPayload.vehicleMake = payload.vehicleMake
        requestPayload.vehicleColor = payload.vehicleColor
        requestPayload.fuelType = payload.fuelType
        requestPayload.registrationDate = payload.registrationDate
        if (payload.engineDisplacement) {
          requestPayload.engineDisplacement = Number(payload.engineDisplacement)
        }
      }

      await registerCustomer(requestPayload)

      const successMsg = hasVehicleInfo()
        ? 'Registration successful! Your vehicle is now verified.'
        : 'Registration successful! You can now add vehicles or be assigned as a driver.'

      toast.success(successMsg)
      navigate('/login')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg ?? 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      {/* Stepper */}
      <div className="flex items-center justify-center mb-8 gap-0">
        {steps.map((s, i) => (
          <div key={s.id} className="flex items-center">
            <div className="flex flex-col items-center gap-1">
              <div
                className={`h-10 w-10 rounded-full flex items-center justify-center font-semibold text-sm border-2 transition-all ${
                  step > s.id
                    ? 'bg-green-500 border-green-500 text-white'
                    : step === s.id
                    ? 'bg-brand-600 border-brand-600 text-white'
                    : 'bg-white border-gray-300 text-gray-400'
                }`}
              >
                {step > s.id ? <Check className="h-5 w-5" /> : <s.icon className="h-4 w-4" />}
              </div>
              <span className={`text-xs font-medium hidden sm:block ${step >= s.id ? 'text-gray-800' : 'text-gray-400'}`}>
                {s.label}
              </span>
            </div>
            {i < steps.length - 1 && (
              <div className={`h-0.5 w-12 sm:w-16 mx-2 mb-4 transition-colors ${step > s.id ? 'bg-green-400' : 'bg-gray-200'}`} />
            )}
          </div>
        ))}
      </div>

      <div className="card">
        <h2 className="text-xl font-bold text-gray-900 mb-6">
          {step === 1 ? 'Personal Information'
            : step === 2 ? 'Verify Mobile Number'
            : step === 3 ? 'Vehicle Details'
            : 'Review & Submit'}
        </h2>

        {/* Step 1: Personal Info */}
        {step === 1 && (
          <div className="space-y-4">
            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="label">Full Name *</label>
                <input className="input-field" placeholder="John Doe" value={form.ownerName} onChange={set('ownerName')} />
              </div>
              <div>
                <label className="label">National ID (NID) *</label>
                <input className="input-field" placeholder="123456789V" value={form.ownerNid} onChange={set('ownerNid')} />
              </div>
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="label">Mobile Number *</label>
                <input className="input-field" type="tel" placeholder="01711123456" value={form.ownerMobile} onChange={set('ownerMobile')} />
              </div>
              <div>
                <label className="label">Email Address *</label>
                <input className="input-field" type="email" placeholder="you@example.com" value={form.ownerEmail} onChange={set('ownerEmail')} />
              </div>
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="label">Password *</label>
                <input className="input-field" type="password" placeholder="Min. 8 characters" value={form.password} onChange={set('password')} />
              </div>
              <div>
                <label className="label">Confirm Password *</label>
                <input className="input-field" type="password" placeholder="Repeat password" value={form.confirmPassword} onChange={set('confirmPassword')} />
              </div>
            </div>
          </div>
        )}

        {/* Step 2: OTP Verification */}
        {step === 2 && (
          <div className="space-y-5">
            <div className="bg-blue-50 border border-blue-200 rounded-lg px-4 py-3">
              <p className="text-sm text-blue-700">
                A 6-digit OTP has been sent to <strong>{form.ownerMobile}</strong>. Please enter it below to verify your mobile number.
              </p>
            </div>

            <div>
              <label className="label">Enter OTP *</label>
              <input
                className="input-field text-center text-2xl tracking-widest font-mono"
                type="text"
                inputMode="numeric"
                maxLength={6}
                placeholder="000000"
                value={form.otp}
                onChange={(e) => {
                  const val = e.target.value.replace(/\D/g, '').slice(0, 6)
                  setForm((f) => ({ ...f, otp: val }))
                }}
              />
              <p className="text-xs text-gray-400 mt-1">Enter the 6-digit code sent to your mobile number.</p>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={handleSendOtp}
                disabled={otpSending}
                className="btn-secondary text-sm"
              >
                {otpSending ? <LoadingSpinner size="sm" /> : null}
                {otpSent ? 'Resend OTP' : 'Send OTP'}
              </button>
              {otpSent && (
                <span className="text-xs text-green-600 font-medium">✓ OTP sent</span>
              )}
            </div>
          </div>
        )}

        {/* Step 3: Vehicle Details */}
        {step === 3 && (
          <div className="space-y-4">
            <div className="bg-blue-50 border border-blue-200 rounded-lg px-4 py-3 mb-4">
              <p className="text-sm text-blue-700">
                <strong>Note:</strong> Vehicle registration is optional. You can skip this step to create a driver-only account and be assigned to vehicles later.
              </p>
            </div>
            <div>
              <label className="label">Registration Number</label>
              <RegistrationNumberInput
                value={{
                  brtaOfficeCode: form.brtaOfficeCode ?? '',
                  vehicleRegistrationCode: form.vehicleRegistrationCode ?? '',
                  serialPart1: form.serialPart1 ?? '',
                  serialPart2: form.serialPart2 ?? '',
                }}
                onChange={(val) => setForm((f) => ({ ...f, ...val }))}
              />
              <p className="text-xs text-gray-400 mt-1">
                Select your BRTA region, vehicle category code, then enter the 2-digit and 4-digit serial numbers.
              </p>
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="label">Vehicle Make</label>
                <input className="input-field" placeholder="Toyota, Honda, etc." value={form.vehicleMake} onChange={set('vehicleMake')} />
              </div>
              <div>
                <label className="label">Vehicle Color</label>
                <input className="input-field" placeholder="Silver, Black, etc." value={form.vehicleColor} onChange={set('vehicleColor')} />
              </div>
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="label">Fuel Type</label>
                <select className="input-field" value={form.fuelType} onChange={set('fuelType')}>
                  {FUEL_TYPES.map((f) => <option key={f}>{f}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Registration Date</label>
                <input className="input-field" type="date" value={form.registrationDate} onChange={set('registrationDate')} />
              </div>
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="label">Engine Displacement <span className="text-gray-400 font-normal">(optional)</span></label>
                <input className="input-field" type="number" placeholder="e.g. 1500" min={50} max={10000}
                  value={form.engineDisplacement ?? ''}
                  onChange={(e) => setForm((f) => ({ ...f, engineDisplacement: e.target.value ? Number(e.target.value) : undefined }))} />
              </div>
            </div>
          </div>
        )}

        {/* Step 4: Review */}
        {step === 4 && (
          <div className="space-y-4">
            <div className="bg-gray-50 rounded-xl p-4 space-y-3">
              <h3 className="font-semibold text-gray-700 text-sm uppercase tracking-wide">Personal Information</h3>
              <dl className="grid grid-cols-2 gap-2 text-sm">
                <div><dt className="text-gray-500">Name</dt><dd className="font-medium">{form.ownerName}</dd></div>
                <div><dt className="text-gray-500">NID</dt><dd className="font-medium">{form.ownerNid}</dd></div>
                <div><dt className="text-gray-500">Mobile</dt><dd className="font-medium">{form.ownerMobile} <span className="text-green-600 text-xs">✓ Verified</span></dd></div>
                <div><dt className="text-gray-500">Email</dt><dd className="font-medium">{form.ownerEmail}</dd></div>
              </dl>
            </div>
            {hasVehicleInfo() ? (
              <>
                <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                  <h3 className="font-semibold text-gray-700 text-sm uppercase tracking-wide">Vehicle Information</h3>
                  <dl className="grid grid-cols-2 gap-2 text-sm">
                    <div><dt className="text-gray-500">Reg. No.</dt><dd className="font-medium font-mono">{assembledRegNumber()}</dd></div>
                    <div><dt className="text-gray-500">Make</dt><dd className="font-medium">{form.vehicleMake}</dd></div>
                    <div><dt className="text-gray-500">Color</dt><dd className="font-medium">{form.vehicleColor}</dd></div>
                    <div><dt className="text-gray-500">Fuel Type</dt><dd className="font-medium">{form.fuelType}</dd></div>
                    <div><dt className="text-gray-500">Reg. Date</dt><dd className="font-medium">{form.registrationDate}</dd></div>
                    {form.engineDisplacement && <div><dt className="text-gray-500">Engine Displacement</dt><dd className="font-medium">{form.engineDisplacement} CC</dd></div>}
                  </dl>
                </div>
                <p className="text-xs text-gray-500 bg-green-50 border border-green-200 rounded-lg px-4 py-3">
                  ✓ Your vehicle will be immediately verified and your fuel quota activated upon submission.
                </p>
              </>
            ) : (
              <div className="bg-blue-50 rounded-xl p-4">
                <p className="text-sm text-blue-700">
                  <strong>Driver-Only Account:</strong> No vehicle will be registered. You can add vehicles later or be assigned as a driver to other vehicles.
                </p>
              </div>
            )}
          </div>
        )}

        {/* Navigation buttons */}
        <div className="flex items-center justify-between mt-6 pt-4 border-t border-gray-100">
          {step > 1 ? (
            <button onClick={() => setStep((s) => s - 1)} className="btn-secondary">Back</button>
          ) : (
            <Link to="/login" className="text-sm text-gray-500 hover:text-gray-700">Already have an account?</Link>
          )}
          <div className="flex gap-2">
            {step === 3 && (
              <button onClick={skipVehicle} className="btn-secondary">
                Skip - Register as Driver Only
              </button>
            )}
            {step < 4 ? (
              <button onClick={next} disabled={otpSending} className="btn-primary gap-2">
                {otpSending ? <LoadingSpinner size="sm" /> : <ChevronRight className="h-4 w-4" />}
                Continue
              </button>
            ) : (
              <button onClick={handleSubmit} disabled={loading} className="btn-primary gap-2">
                {loading ? <LoadingSpinner size="sm" /> : <Check className="h-4 w-4" />}
                Submit Registration
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

