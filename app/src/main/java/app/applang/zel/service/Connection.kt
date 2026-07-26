package app.applang.zel.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import app.applang.zel.IUserService

class Connection : ServiceConnection {

    var SERVICE: IUserService? = null
    fun set(service: IUserService?) {
        if (SERVICE == null) {
            SERVICE = service
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        set(IUserService.Stub.asInterface(service))
        UserServiceProvider.onConnectionChanged()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        SERVICE = null
        UserServiceProvider.onConnectionChanged()
    }
}