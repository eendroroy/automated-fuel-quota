import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import en from './locales/en.json'
import bn from './locales/bn.json'

export const SUPPORTED_LANGUAGES = [
  { code: 'bn', label: 'বাংলা', labelEn: 'Bangla' },
  { code: 'en', label: 'English', labelEn: 'English' },
] as const

export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number]['code']

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      bn: { translation: bn },
    },
    // Bangla is the default language
    fallbackLng: 'bn',
    lng: localStorage.getItem('language') || 'bn',
    detection: {
      order: ['localStorage'],
      caches: ['localStorage'],
      lookupLocalStorage: 'language',
    },
    interpolation: {
      escapeValue: false,
    },
  })

export default i18n

