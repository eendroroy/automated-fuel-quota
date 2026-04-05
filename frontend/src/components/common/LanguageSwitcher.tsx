import { useTranslation } from 'react-i18next'
import { SUPPORTED_LANGUAGES } from '@/i18n'

/**
 * Language pill-toggle: shows both language options side by side.
 * The active language is highlighted; clicking either segment switches immediately
 * and persists the choice to localStorage.
 */
export default function LanguageSwitcher() {
  const { i18n } = useTranslation()

  const switchTo = (code: string) => {
    i18n.changeLanguage(code)
    localStorage.setItem('language', code)
  }

  return (
    <div
      className="flex items-center bg-gray-100 dark:bg-gray-800 rounded-lg p-0.5 gap-0.5 select-none"
      role="group"
      aria-label="Language switcher"
    >
      {SUPPORTED_LANGUAGES.map((lang) => {
        const isActive = i18n.language === lang.code
        return (
          <button
            key={lang.code}
            onClick={() => switchTo(lang.code)}
            title={lang.labelEn}
            aria-pressed={isActive}
            className={`flex items-center gap-1 px-2 py-1 rounded-md text-xs font-semibold transition-all ${
              isActive
                ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
            }`}
          >
            <span className="text-sm leading-none">
              {lang.code === 'bn' ? '🇧🇩' : '🇬🇧'}
            </span>
            <span className="hidden sm:inline">{lang.code === 'bn' ? 'বাং' : 'EN'}</span>
          </button>
        )
      })}
    </div>
  )
}
