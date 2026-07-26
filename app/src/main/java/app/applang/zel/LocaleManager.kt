package app.applang.zel

import app.applang.zel.ui.screen.appinfo.LocaleRegion
import app.applang.zel.ui.screen.appinfo.SingleLocale
import app.applang.zel.ui.screen.appinfo.capDisplayName
import java.util.Locale

class LocaleManager {

    val localeList: List<LocaleRegion>

    init {
        val byLanguage = linkedMapOf<String, LocaleRegion>()
        for (locale in Locale.getAvailableLocales()) {
            val language = locale.getDisplayLanguage(locale)
                .replaceFirstChar { it.uppercaseChar() }
            if (language.isEmpty()) continue

            // getOrPut, because the previous version created the region with an empty list and
            // only started appending from the *second* locale of each language: every language
            // was silently missing one of its regional variants.
            val region = byLanguage.getOrPut(language) { LocaleRegion(language, mutableListOf()) }
            region.locales.add(SingleLocale(locale.capDisplayName(), locale.toLanguageTag()))
        }

        byLanguage.values.forEach { region -> region.locales.sortBy { it.name } }
        localeList = byLanguage.values.sortedBy { it.language }
    }
}
