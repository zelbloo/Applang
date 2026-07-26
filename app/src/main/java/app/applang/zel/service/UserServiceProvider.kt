package app.applang.zel.service

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.applang.zel.IUserService
import app.applang.zel.ui.screen.main.OperationMode

object UserServiceProvider {

    private val tag = this.javaClass.simpleName

    var connection = Connection()
    var opMode = OperationMode.NONE

    private val _isServiceConnected = MutableStateFlow(false)

    /**
     * Whether the privileged service is currently bound.
     *
     * Permission is granted outside the app — Shizuku shows its own dialog, root is granted by
     * the superuser manager — so nothing calls back into the UI when it happens. Screens
     * observe this instead of waiting for the user to restart the app.
     */
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    /** Called by [Connection] whenever the binder is handed over or lost. */
    fun onConnectionChanged() {
        _isServiceConnected.value = isConnected()
    }

    // Blocking
    fun getService(): IUserService {
        var timeout = 0
        while (!isConnected()) {
            timeout += 1000
            if (timeout > 20000) {
                throw Exception("Service unavailable.")
            }
            Thread.sleep(1000)
        }
        return this.connection.SERVICE!!
    }

    fun run(
        onFail: () -> Unit = {},
        onConnected: suspend IUserService.() -> Unit,
    ) {
        fun service() = connection.SERVICE!!
        CoroutineScope(Dispatchers.IO).launch {
            if (isConnected()) {
                onConnected(service())
                return@launch
            }
            var timeout = 0
            while (!isConnected()) {
                timeout += 1000
                if (timeout > 20000) {
                    Log.e(tag, "Service unavailable.")
                    onFail()
                    return@launch
                }
                delay(1000)
                Log.d(tag, "Service unavailable, checking again in 1s.. [${timeout / 1000}s/20s]")
            }
            val serviceUid = service().uid
            Log.d(tag, "IUserService available, uid: $serviceUid")
            if(serviceUid == 0)
                opMode = OperationMode.ROOT
            if(serviceUid <= 2000)
                opMode = OperationMode.SHIZUKU
            onConnected(service())
        }
    }

    fun isConnected(): Boolean {
        return connection.SERVICE != null
    }
}
