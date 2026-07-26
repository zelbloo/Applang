package app.applang.zel.ui.screen.appinfo

import android.graphics.drawable.Drawable
import java.util.Locale

data class LocaleRegion(
    val language: String,
    val locales: MutableList<SingleLocale>
)

data class SingleLocale(
    val name: String,
    val languageTag: String
) {
    fun toLocale(): Locale {
        return Locale.forLanguageTag(languageTag)
    }
}

data class AppInfoState(
    val appIcon: Drawable? = null,
    val appName: String = "",
    val appPackage: String = "",
    val currentLanguage: String = "",
    val listOfSuggestedLanguages: List<SingleLocale> = emptyList(),
    val listOfPinnedLanguages: List<SingleLocale> = emptyList(),
    val selectedLanguage: Int = -1,
    val listOfAllLanguages: List<LocaleRegion> = emptyList(),
)
