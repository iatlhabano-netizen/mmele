package bw.mmele

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.tshipiworks.mmele.R

/**
 * In-app language choice, stored by Android's per-app locale support (Settings > Apps > Mmele >
 * Language shows the same value on Android 13+; AppCompat persists it on older versions).
 */
enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.lang_system),
    ENGLISH("en", R.string.lang_english),
    SETSWANA("tn", R.string.lang_setswana);

    fun apply() {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }

    companion object {
        fun current(): AppLanguage {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) return SYSTEM
            val lang = locales[0]?.language.orEmpty()
            return entries.firstOrNull { it.tag.isNotEmpty() && it.tag == lang } ?: SYSTEM
        }
    }
}
