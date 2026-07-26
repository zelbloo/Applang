package app.applang.zel

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku
import app.applang.zel.service.RootUserService
import app.applang.zel.service.UserService
import app.applang.zel.service.UserServiceProvider
import app.applang.zel.ui.screen.Navigation
import app.applang.zel.ui.screen.main.OperationMode
import app.applang.zel.ui.theme.LanguageSelectorTheme

object ShizukuArgs {
    val userServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, UserService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
}


@AndroidEntryPoint
class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    init {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(Shell.Builder.create().setTimeout(10))
    }

    private val shizukuRequestCode = 1

    /** Kept so a dialog the user declined is not thrown at them again on every resume. */
    private var hasRequestedShizukuPermission = false

    private fun bindShizuku() {
        Shizuku.bindUserService(ShizukuArgs.userServiceArgs, UserServiceProvider.connection)
    }

    private val REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionResult

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == PackageManager.PERMISSION_GRANTED)
            bindShizuku()
    }

    /**
     * Shizuku is a separate app that may not have been running when this activity was created,
     * so this runs on every resume rather than once in onCreate. It used to run only in
     * onCreate and only when the binder was already alive, which meant that starting Shizuku
     * after opening this app left it with no way to ever ask for permission.
     *
     * @param userInitiated the user asked for this from the permission dialog, so ask again
     *   even if we already have.
     */
    private fun connectShizukuIfPossible(userInitiated: Boolean = false) {
        if (!Shizuku.pingBinder() || UserServiceProvider.isConnected()) return

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindShizuku()
            return
        }

        if (userInitiated || !hasRequestedShizukuPermission) {
            hasRequestedShizukuPermission = true
            Shizuku.requestPermission(shizukuRequestCode)
        }
    }

    /** Called from the "permissions required" dialog. */
    fun requestShizukuAccess() = connectShizukuIfPossible(userInitiated = true)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Drives the system bar icon colours from the theme, so they stay legible when the
        // system switches between light and dark mode.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // On a configuration change the request was already made by the previous instance.
        hasRequestedShizukuPermission = savedInstanceState != null

        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)

        setContent {
            LanguageSelectorTheme { Navigation() }
        }

        RootReceivedListener.setListener(object : IRootListener {
            override fun onRootReceived() {
                val intent = Intent(application, RootUserService::class.java)
                RootService.bind(intent, UserServiceProvider.connection)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        connectShizukuIfPossible()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
        RootReceivedListener.destroy()
        if (UserServiceProvider.isConnected()) {
            when (UserServiceProvider.opMode) {
                OperationMode.ROOT -> RootService.unbind(UserServiceProvider.connection)
                OperationMode.SHIZUKU -> Shizuku.unbindUserService(
                    ShizukuArgs.userServiceArgs,
                    UserServiceProvider.connection,
                    true
                )

                else -> Log.d(BuildConfig.APPLICATION_ID, "UserService not bound.")
            }
        }
        super.onDestroy()
    }

}