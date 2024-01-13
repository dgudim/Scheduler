package prototype.xd.scheduler.utilities;

import static androidx.core.content.UnusedAppRestrictionsConstants.API_30;
import static androidx.core.content.UnusedAppRestrictionsConstants.API_30_BACKPORT;
import static androidx.core.content.UnusedAppRestrictionsConstants.API_31;
import static androidx.core.content.UnusedAppRestrictionsConstants.DISABLED;
import static androidx.core.content.UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Static.PACKAGE_NAME;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public final class PermissionUtilities {
    
    public static final String NAME = PermissionUtilities.class.getSimpleName();
    
    public enum AutoRevokeStatus {
        ENABLED, DISABLED, AVAILABLE, NOT_AVAILABLE
    }
    
    private PermissionUtilities() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    private static final String[] permissions = isOnAndroid13OrHigher() ?
            new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.POST_NOTIFICATIONS} :
            new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CALENDAR};
    
    @NonNull
    public static String[] getPermissions() {
        return permissions;
    }
    
    public static boolean isOnAndroid13OrHigher() {
        // TODO: we are targeting Android 12 for now because of the broken api, so this function shouldn't return true
        return Build.VERSION.SDK_INT >= 1000;
    }
    
    /**
     * Checks if essential permissions (storage and calendar) are granted
     *
     * @param context any context
     * @return true if essential permissions are granted
     */
    public static boolean areEssentialPermissionsGranted(@NonNull Context context) {
        boolean granted = isStorageGranted(context) && isCalendarGranted(context);
        Logger.debug(NAME, "Essential permissions" + (granted ? "" : " not") + " granted");
        return granted;
    }
    
    /**
     * Checks if storage read is granted
     *
     * @param context any context
     * @return true if storage read permission is granted
     */
    public static boolean isStorageGranted(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Checks if calendar read permission is granted
     *
     * @param context any context
     * @return true if calendar read permission is granted
     */
    public static boolean isCalendarGranted(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Checks if battery permission is granted
     *
     * @param wrapper context wrapper
     * @return true if battery permission is granted
     */
    public static boolean isBatteryGranted(@NonNull ContextWrapper wrapper) {
        return ((PowerManager) wrapper.activity
                .getSystemService(Context.POWER_SERVICE))
                .isIgnoringBatteryOptimizations(PACKAGE_NAME);
    }
    
    /**
     * Checks if notification permission is granted
     *
     * @param wrapper context wrapper
     * @return true if notification permission is granted
     */
    public static boolean isNotificationGranted(@NonNull ContextWrapper wrapper) {
        if (!isOnAndroid13OrHigher()) {
            return true;
        }
        return ContextCompat.checkSelfPermission(wrapper.context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Gets application autorevoke status
     *
     * @param wrapper context wrapper
     * @return {@link AutoRevokeStatus}
     */
    // areUnusedAppRestrictionsAvailable is a restricted API for some reason
    @SuppressLint("RestrictedApi")
    @NonNull
    public static AutoRevokeStatus getAutorevokeStatus(@NonNull ContextWrapper wrapper) {
        Context context = wrapper.context;
        try {
            int uStatus = PackageManagerCompat.getUnusedAppRestrictionsStatus(context).get(10, TimeUnit.MILLISECONDS);
            if (uStatus == FEATURE_NOT_AVAILABLE) {
                return AutoRevokeStatus.NOT_AVAILABLE;
            } else if (uStatus == API_30_BACKPORT || uStatus == API_30 || uStatus == API_31) {
                return AutoRevokeStatus.ENABLED;
            } else if (uStatus == DISABLED) {
                return AutoRevokeStatus.DISABLED;
            }
        } catch (ExecutionException | CancellationException | TimeoutException e) {
            logException(Thread.currentThread().getName(), e);
        } catch (InterruptedException e) {
            logException(Thread.currentThread().getName(), e);
            Thread.currentThread().interrupt();
        }
        if (PackageManagerCompat.areUnusedAppRestrictionsAvailable(context.getPackageManager())) {
            // trying this as a fallback
            return AutoRevokeStatus.AVAILABLE;
        }
        return AutoRevokeStatus.NOT_AVAILABLE;
    }
    
    public static boolean isAutorevokeGranted(@NonNull ContextWrapper wrapper) {
        AutoRevokeStatus status = getAutorevokeStatus(wrapper);
        return status == AutoRevokeStatus.DISABLED || status == AutoRevokeStatus.NOT_AVAILABLE;
    }
}
