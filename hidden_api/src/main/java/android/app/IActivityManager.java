package android.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IActivityManager extends IInterface {

    void forceStopPackage(String packageName, int userId);

    /**
     * Available since Android 12. This is what the hidden
     * {@code ActivityManager.getCurrentUser()} helper delegates to; calling it through the
     * binder interface avoids stubbing {@code android.app.ActivityManager}, whose public
     * counterpart on the platform android.jar shadows any local stub at compile time.
     */
    int getCurrentUserId();

    abstract class Stub extends Binder implements IActivityManager {

        public static IActivityManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }

    }
}