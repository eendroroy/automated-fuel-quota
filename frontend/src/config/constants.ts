export const WEEKLY_QUOTA_LITRES = 24
export const TOKEN_KEY = 'afq_token'
export const USER_KEY = 'afq_user'
export const DEFAULT_PAGE_SIZE = 10

export const VEHICLE_CLASSES = [
  'Private Car',
  'Motorcycle',
  'Bus',
  'Truck',
  'Three-Wheeler',
  'Van',
  'Pick-Up',
  'Lorry',
]

export const FUEL_TYPES = ['Petrol', 'Diesel', 'Octane', 'CNG', 'LPG']

export const QUOTA_PERIODS = ['DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'] as const
export type QuotaPeriodOption = typeof QUOTA_PERIODS[number]

export const DISTRICTS = [
  'Dhaka',
  'Chittagong',
  'Rajshahi',
  'Khulna',
  'Sylhet',
  'Barishal',
  'Mymensingh',
  'Rangpur',
  'Comilla',
  'Gazipur',
  'Narayanganj',
  'Tangail',
]

