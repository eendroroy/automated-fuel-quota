import { useTranslation } from 'react-i18next'
import { SUPPORTED_LANGUAGES } from '@/i18n'

export default function LanguageSwitcher() {
  const { i18n } = useTranslation()

  const current = SUPPORTED_LANGUAGES.find((l) => l.code === i18n.language) ?? SUPPORTED_LANGUAGES[0]
  const other = SUPPORTED_LANGUAGES.find((l) => l.code !== i18n.language) ?? SUPPORTED_LANGUAGES[1]

  const toggle = () => {
    const next = other.code
    i18n.changeLanguage(next)
    localStorage.setItem('language', next)
  }

  return (
    <button
      onClick={toggle}
      title={`Switch to ${other.labelEn}`}
      className="flex items-center gap-1.5 text-sm font-medium px-2.5 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors select-none"
      aria-label={`Switch language to ${other.labelEn}`}
    >
      <span className="text-base leading-none">{current.code === 'bn' ? '🇧🇩' : '🇬🇧'}</span>
      <span className="hidden sm:inline">{other.label}</span>
    </button>
  )
}

