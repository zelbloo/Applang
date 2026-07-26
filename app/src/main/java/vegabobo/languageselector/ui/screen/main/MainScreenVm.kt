package vegabobo.languageselector.ui.screen.main

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import vegabobo.languageselector.BuildConfig
import vegabobo.languageselector.RootReceivedListener
import vegabobo.languageselector.dao.AppInfoDb
import vegabobo.languageselector.service.UserServiceProvider
import javax.inject.Inject


@HiltViewModel
class MainScreenVm @Inject constructor(
    val app: Application,
    appInfoDb: AppInfoDb
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()
    var lastSelectedApp: AppInfo? = null
        private set
    private val dao = appInfoDb.appInfoDao()

    fun loadOperationMode() {
        if (Shell.getShell().isAlive)
            Shell.getShell().close()
        Shell.getShell()
        if (Shell.isAppGrantedRoot() == true) {
            _uiState.update { it.copy(operationMode = OperationMode.ROOT) }
            RootReceivedListener.onRootReceived()
            return
        }

        val isAvail = Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        if (isAvail) {
            _uiState.update { it.copy(operationMode = OperationMode.SHIZUKU) }
            return
        }

        _uiState.update { it.copy(operationMode = OperationMode.NONE) }
    }

    init {
        fillListOfApps()
        reloadWhenServiceConnects()
    }

    /**
     * Permission is granted outside this screen, so nothing tells us when it happens. Watch the
     * service instead: as soon as it binds, load the list the first attempt had to give up on.
     *
     * The current value is dropped because it needs no handling — if the service was already
     * connected when this view model was created, the load started in [init] succeeds on its own.
     */
    private fun reloadWhenServiceConnects() {
        viewModelScope.launch {
            UserServiceProvider.isServiceConnected
                .drop(1)
                .filter { isConnected -> isConnected }
                .collect {
                    if (_uiState.value.operationMode != OperationMode.NONE) return@collect
                    _uiState.update { it.copy(isLoading = true) }
                    fillListOfApps()
                }
        }
    }

    fun parseAppInfo(a: ApplicationInfo): AppInfo {
        val isSystemApp = (a.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val service = UserServiceProvider.getService()
        val languagePreferences = service.getApplicationLocales(a.packageName)
        val labels = arrayListOf<AppLabels>()
        if (isSystemApp)
            labels.add(AppLabels.SYSTEM_APP)
        if (!languagePreferences.isEmpty)
            labels.add(AppLabels.MODIFIED)
        return AppInfo(
            icon = app.packageManager.getAppIcon(a),
            name = app.packageManager.getLabel(a),
            pkg = a.packageName,
            labels = labels
        )
    }

    fun fillListOfApps() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.operationMode == OperationMode.NONE)
                loadOperationMode()

            if (_uiState.value.operationMode == OperationMode.NONE) {
                // Nothing can be read without Shizuku or root. Stop here so the UI leaves the
                // loading state and shows the permission dialog; parseAppInfo() would otherwise
                // block for 20s waiting for a service that will never connect, and then throw.
                // reloadWhenServiceConnects() picks things up once access is granted.
                _uiState.update {
                    it.copy(listOfApps = emptyList(), isLoading = false, isRefreshing = false)
                }
                return@launch
            }

            val sortedList = runCatching {
                getInstalledPackages().map { parseAppInfo(it) }.sortedApps()
            }.getOrElse {
                Log.e(BuildConfig.APPLICATION_ID, "Could not read the installed apps", it)
                emptyList()
            }
            _uiState.update {
                it.copy(listOfApps = sortedList, isLoading = false, isRefreshing = false)
            }
        }
    }

    /**
     * Pull to refresh. Re-detects the operation mode when there is none, so this doubles as the
     * manual retry when the automatic reload did not catch the permission being granted.
     */
    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        fillListOfApps()
    }

    /** Modified apps first, alphabetically within each group. */
    private fun List<AppInfo>.sortedApps(): List<AppInfo> =
        sortedWith(compareBy({ !it.isModified() }, { it.name.lowercase() }))

    fun getInstalledPackages(): List<ApplicationInfo> {
        return app.packageManager.getInstalledApplications(
            PackageManager.ApplicationInfoFlags.of(0)
        ).mapNotNull {
            if (!it.enabled || BuildConfig.APPLICATION_ID == it.packageName)
                null
            else
                it
        }
    }

    fun toggleDropdown() {
        _uiState.update { it.copy(isDropdownVisible = !it.isDropdownVisible) }
    }

    fun toggleSystemAppsVisibility() {
        _uiState.update {
            it.copy(
                isShowSystemAppsHome = !it.isShowSystemAppsHome,
                isDropdownVisible = false
            )
        }
    }

    /**
     * Retry after the user has granted Shizuku access. This has to reload the app list too:
     * the first attempt gave up before reading anything.
     */
    fun onClickProceedShizuku() {
        _uiState.update { it.copy(isLoading = true) }
        fillListOfApps()
    }

    fun updateHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val historyPkgs = dao.getHistory().map { it.pkg }
            val apps = _uiState.value.listOfApps.associateBy { it.pkg }
            val history = historyPkgs.mapNotNull { apps[it] }
            _uiState.update { it.copy(history = history) }
        }
    }

    private fun addAppToHistory(ai: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.findByPkg(ai.pkg) == null) {
                dao.insert(ai.toAppInfoEntity())
            }
            dao.setLastSelected(ai.pkg, System.currentTimeMillis())
            updateHistory()
        }
    }

    fun onClickClear() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.cleanLastSelectedAll()
            updateHistory()
        }
    }

    /**
     * Called when coming back from the app language screen: the app may have gained or lost its
     * locale override, which moves it to the other end of the list.
     */
    fun reloadLastSelectedItem() {
        val selected = lastSelectedApp ?: return
        // parseAppInfo() talks to the privileged service and can block; keep it off the main
        // thread, where this used to run straight from composition.
        viewModelScope.launch(Dispatchers.IO) {
            val updated = runCatching {
                parseAppInfo(app.packageManager.getApplicationInfo(selected.pkg, 0))
            }.getOrElse {
                Log.e(BuildConfig.APPLICATION_ID, "Could not refresh ${selected.pkg}", it)
                return@launch
            }
            val apps = _uiState.value.listOfApps
            val index = apps.indexOfFirst { it.pkg == updated.pkg }
            if (index == -1 || apps[index].labels == updated.labels) return@launch

            val newList = apps.toMutableList().apply { this[index] = updated }.sortedApps()
            _uiState.update {
                it.copy(
                    listOfApps = newList,
                    snackBarDisplay = if (updated.isModified()) SnackBarDisplay.MOVED_TO_TOP
                    else SnackBarDisplay.MOVED_TO_BOTTOM
                )
            }
        }
    }

    fun resetSnackBarDisplay() = _uiState.update { it.copy(snackBarDisplay = SnackBarDisplay.NONE) }

    fun onClickApp(ai: AppInfo) {
        lastSelectedApp = ai
        addAppToHistory(ai)
    }
}
