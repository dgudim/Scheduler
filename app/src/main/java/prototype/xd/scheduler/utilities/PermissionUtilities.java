package prototype.xd.scheduler.utilities;

import static androidx.core.content.UnusedAppRestrictionsConstants.DISABLED;
import static androidx.core.content.UnusedAppRestrictionsConstants.ERROR;
import static androidx.core.content.UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE;
import static prototype.xd.scheduler.utilities.Static.PACKAGE_NAME;
import static prototype.xd.scheduler.utilities.Logger.logException;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.fragment.app.Fragment;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public final class PermissionUtilities {
    
    public static final String NAME = PermissionUtilities.class.getSimpleName();
    
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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
    
    public static boolean areEssentialPermissionsGranted(@NonNull Context context) {
        boolean granted = isStorageGranted(context) && isCalendarGranted(context);
        Logger.debug(NAME, "Essential permissions" + (granted ? "" : " not") + " granted");
        return granted;
    }
    
    public static boolean isStorageGranted(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }
    
    public static boolean isCalendarGranted(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }
    
    public static boolean isBatteryGranted(@NonNull Fragment fragment) {
        return ((PowerManager) fragment.requireActivity()
                .getSystemService(Context.POWER_SERVICE))
                .isIgnoringBatteryOptimizations(PACKAGE_NAME);
    }
    
    public static boolean isNotificationGranted(@NonNull Fragment fragment) {
        if (!isOnAndroid13OrHigher()) {
            return true;
        }
        return ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
    
    @NonNull
    public static Integer getAutorevokeStatus(@NonNull Fragment fragment) {
        try {
            return PackageManagerCompat.getUnusedAppRestrictionsStatus(fragment.requireContext()).get();
        } catch (ExecutionException | CancellationException e) {
            logException(Thread.currentThread().getName(), e);
        } catch (InterruptedException e) {
            logException(Thread.currentThread().getName(), e);
            Thread.currentThread().interrupt();
        }
        return ERROR;
    }
    
    public static boolean isAutorevokeGranted(@NonNull Fragment fragment) {
        int status = getAutorevokeStatus(fragment);
        return status == FEATURE_NOT_AVAILABLE || status == DISABLED;
    }
    
}
