package app.applang.zel.ui.screen.main

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import app.applang.zel.dao.AppInfoEntity

enum class OperationMode {
    NONE, SHIZUKU, ROOT
}

enum class SnackBarDisplay {
    NONE, MOVED_TO_TOP, MOVED_TO_BOTTOM
}

data class MainScreenState(
    val listOfApps: List<AppInfo> = emptyList(),
    val history: List<AppInfo> = emptyList(),
    val operationMode: OperationMode = OperationMode.NONE,
    val isDropdownVisible: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isShowSystemAppsHome: Boolean = false,
    val snackBarDisplay: SnackBarDisplay = SnackBarDisplay.NONE,
)

enum class AppLabels {
    SYSTEM_APP, MODIFIED
}

data class AppInfo(
    val icon: Drawable,
    val name: String,
    val pkg: String,
    val labels: List<AppLabels> = emptyList()
) {
    fun isSystemApp() = labels.contains(AppLabels.SYSTEM_APP)
    fun isModified() = labels.contains(AppLabels.MODIFIED)
}

/**
 * Apps shown on the home list. System apps are hidden unless asked for, but a system app that
 * already has a locale override always stays visible so it can be undone.
 */
fun List<AppInfo>.filterForHome(showSystemApps: Boolean): List<AppInfo> =
    filter { showSystemApps || !it.isSystemApp() || it.isModified() }

/** Apps matching the search query and the active filter chips. */
fun List<AppInfo>.filterForSearch(
    query: String,
    showSystemApps: Boolean,
    onlyModified: Boolean,
): List<AppInfo> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isEmpty()) return emptyList()
    return filter { app ->
        (showSystemApps || !app.isSystemApp()) &&
                (!onlyModified || app.isModified()) &&
                (app.name.lowercase().contains(normalizedQuery) ||
                        app.pkg.lowercase().contains(normalizedQuery))
    }
}

fun AppInfo.toAppInfoEntity(): AppInfoEntity {
    return AppInfoEntity(this.pkg, this.name, System.currentTimeMillis())
}

fun PackageManager.getLabel(applicationInfo: ApplicationInfo): String {
    return applicationInfo.loadLabel(this).toString()
}

fun PackageManager.getAppIcon(applicationInfo: ApplicationInfo): Drawable {
    return this.getApplicationIcon(applicationInfo)
}
