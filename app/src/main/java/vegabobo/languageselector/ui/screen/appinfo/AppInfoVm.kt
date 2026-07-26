package vegabobo.languageselector.ui.screen.appinfo

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.LocaleList
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vegabobo.languageselector.BuildConfig
import vegabobo.languageselector.LocaleManager
import vegabobo.languageselector.service.UserServiceProvider
import vegabobo.languageselector.ui.screen.main.getAppIcon
import vegabobo.languageselector.ui.screen.main.getLabel
import java.util.Locale
import javax.inject.Inject

object PrefConstants {
    const val PINNED_LOCALES = "pinned_locales"
}


@HiltViewModel
class AppInfoVm @Inject constructor(
    val app: Application,
    private val localeManager: LocaleManager,
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppInfoState())
    val uiState: StateFlow<AppInfoState> = _uiState.asStateFlow()

    lateinit var appInfo: ApplicationInfo

    fun initFromAppId(appId: String) {
        appInfo =
            app.packageManager.getApplicationInfo(appId, PackageManager.ApplicationInfoFlags.of(0))
        _uiState.update {
            it.copy(
                appName = app.packageManager.getLabel(appInfo),
                appPackage = appInfo.packageName,
                appIcon = app.packageManager.getAppIcon(appInfo),
                listOfAllLanguages = localeManager.localeList
            )
        }

        UserServiceProvider.run {
            val suggested = (0 until systemLocales.size()).map { index ->
                val locale = systemLocales[index]
                SingleLocale(locale.capDisplayName(), locale.toLanguageTag())
            }
            _uiState.update { it.copy(listOfSuggestedLanguages = suggested) }
            updateCurrentLanguageState()
        }
    }

    fun updateCurrentLanguageState() {
        UserServiceProvider.run {
            val currentLocale = getApplicationLocales(appInfo.packageName)
            // Also clears the value when the override is removed; it used to keep showing the
            // previous language until the screen was reopened.
            val name = if (currentLocale.isEmpty) "" else currentLocale.get(0).capDisplayName()
            _uiState.update { it.copy(currentLanguage = name) }
        }
    }

    fun onClickSingleLanguage(index: Int) {
        _uiState.update { it.copy(selectedLanguage = index) }
    }

    fun onBackWhenSelectedLang() {
        _uiState.update { it.copy(selectedLanguage = -1) }
    }

    fun onClickLocale(singleLocale: SingleLocale) {
        UserServiceProvider.run {
            setApplicationLocales(
                appInfo.packageName,
                LocaleList(singleLocale.toLocale())
            )
            updateCurrentLanguageState()
        }
    }

    fun onClickSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", appInfo.packageName, null)
        intent.setData(uri)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(intent)
    }

    fun onClickOpen() {
        val launchIntent =
            app.packageManager.getLaunchIntentForPackage(appInfo.packageName) ?: return
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(launchIntent)
    }

    fun onClickResetLang() {
        UserServiceProvider.run {
            setApplicationLocales(appInfo.packageName, LocaleList())
            updateCurrentLanguageState()
        }
    }

    fun onClickForceClose() {
        UserServiceProvider.run {
            forceStopPackage(appInfo.packageName)
        }
    }

    fun isPinned(singleLocale: SingleLocale): Boolean =
        _uiState.value.listOfPinnedLanguages.any { it.languageTag == singleLocale.languageTag }

    fun onTogglePin(singleLocale: SingleLocale) {
        if (isPinned(singleLocale)) onRemovePin(singleLocale) else onPinLang(singleLocale)
    }

    fun onPinLang(singleLocale: SingleLocale) {
        val stored = sharedPreferences.pinnedLocales()
        sharedPreferences.edit {
            putStringSet(
                PrefConstants.PINNED_LOCALES,
                stored + "${singleLocale.name},${singleLocale.languageTag}"
            )
        }
        updatePinnedLangsFromSP()
    }

    fun onRemovePin(singleLocale: SingleLocale) {
        val stored = sharedPreferences.pinnedLocales()
        sharedPreferences.edit {
            putStringSet(
                PrefConstants.PINNED_LOCALES,
                // Compare the tag exactly. This used to be a `contains(languageTag)` over the
                // whole "name,tag" entry, so unpinning "en" also dropped "en-US", "en-GB" and
                // every entry whose display name happened to contain the letters.
                stored.filterNot { it.substringAfterLast(',') == singleLocale.languageTag }
                    .toSet()
            )
        }
        updatePinnedLangsFromSP()
    }

    fun updatePinnedLangsFromSP() {
        val pinnedLocaleList = sharedPreferences.pinnedLocales().parseSetLangs()
        _uiState.update { it.copy(listOfPinnedLanguages = pinnedLocaleList) }
    }

    private fun SharedPreferences.pinnedLocales(): Set<String> =
        getStringSet(PrefConstants.PINNED_LOCALES, emptySet()).orEmpty()
}

fun Locale.capDisplayName(): String {
    return this.getDisplayName(this).replaceFirstChar { it.uppercaseChar() }
}

/**
 * Pinned locales are stored as "display name,languageTag". Split on the last comma: language
 * tags never contain one, but display names sometimes do.
 */
fun Set<String>.parseSetLangs(): List<SingleLocale> {
    return this.mapNotNull { entry ->
        val tag = entry.substringAfterLast(',', missingDelimiterValue = "")
        if (tag.isEmpty()) {
            Log.e(BuildConfig.APPLICATION_ID, "Malformed pinned locale entry: $entry")
            return@mapNotNull null
        }
        SingleLocale(entry.substringBeforeLast(','), tag)
    }.sortedBy { it.name }
}
