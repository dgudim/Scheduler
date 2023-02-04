package prototype.xd.scheduler;

import static android.app.Activity.RESULT_CANCELED;
import static androidx.core.content.UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE;
import static prototype.xd.scheduler.utilities.Static.PACKAGE_NAME;
import static prototype.xd.scheduler.utilities.PermissionUtilities.areEssentialPermissionsGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.getAutorevokeStatus;
import static prototype.xd.scheduler.utilities.PermissionUtilities.getPermissions;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isAutorevokeGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isBatteryGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isCalendarGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isNotificationGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isOnAndroid13OrHigher;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isStorageGranted;
import static prototype.xd.scheduler.utilities.Utilities.displayToast;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.IntentCompat;
import androidx.fragment.app.Fragment;

import com.github.appintro.SlidePolicy;
import com.google.android.material.color.MaterialColors;

import prototype.xd.scheduler.databinding.PermissionsRequestFragmentBinding;
import prototype.xd.scheduler.utilities.GraphicsUtilities;


public class PermissionRequestFragment extends Fragment implements SlidePolicy { // NOSONAR this is a fragment
    
    private PermissionsRequestFragmentBinding bnd;
    
    private final ActivityResultLauncher<Intent> intentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result ->
                    onActivityResult(result.getResultCode() == RESULT_CANCELED));
    
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result ->
                    onActivityResult(result.containsValue(Boolean.FALSE))
    );
    
    @Override
    @MainThread
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bnd = PermissionsRequestFragmentBinding.inflate(inflater, container, false);
        displayPermissions();
        
        if (!isOnAndroid13OrHigher()) {
            bnd.notificationPermissionIcon.setVisibility(View.GONE);
            bnd.notificationPermissionTitle.setVisibility(View.GONE);
            bnd.notificationPermissionDescription.setVisibility(View.GONE);
            bnd.notificationPermissionGranted.setVisibility(View.GONE);
        }
        
        if (getAutorevokeStatus(this) == FEATURE_NOT_AVAILABLE) {
            bnd.ignoreAutorevokeIcon.setVisibility(View.GONE);
            bnd.ignoreAutorevokeTitle.setVisibility(View.GONE);
            bnd.ignoreAutorevokeDescription.setVisibility(View.GONE);
            bnd.ignoreAutorevokeGranted.setVisibility(View.GONE);
        }
        
        bnd.grantPermissionsButton.setOnClickListener(v -> requestPermissions(false));
        
        return bnd.getRoot();
    }
    
    private void onActivityResult(boolean userRejected) {
        displayPermissions();
        requestPermissions(userRejected);
    }
    
    @SuppressLint("BatteryLife")
    private void requestPermissions(boolean userRejected) {
        
        boolean storageGranted = isStorageGranted(requireContext());
        boolean calendarGranted = isCalendarGranted(requireContext());
        boolean essentialsGranted = storageGranted && calendarGranted;
        
        if (!essentialsGranted) {
            if (userRejected) {
                displayGrantPermissionsToast();
            } else {
                requestPermissionLauncher.launch(getPermissions());
            }
            return;
        }
        
        if (userRejected) {
            return;
        }
        
        if (!isBatteryGranted(this)) {
            intentLauncher.launch(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + PACKAGE_NAME)));
            return;
        }
        
        if (!isAutorevokeGranted(this)) {
            displayToast(requireContext(), R.string.autorevoke_request_description);
            intentLauncher.launch(IntentCompat.createManageUnusedAppRestrictionsIntent(requireContext(), PACKAGE_NAME));
        }
    }
    
    private void displayPermissions() {
        
        boolean notificationsGranted = isNotificationGranted(this);
        
        boolean batteryGranted = isBatteryGranted(this);
        boolean storageGranted = isStorageGranted(requireContext());
        boolean calendarGranted = isCalendarGranted(requireContext());
        boolean autorevokeGranted = isAutorevokeGranted(this);
        
        boolean allGranted = storageGranted && calendarGranted && batteryGranted && notificationsGranted && autorevokeGranted;
        
        setPermissionChipColor(calendarGranted, bnd.calendarPermissionGranted);
        setPermissionChipColor(storageGranted, bnd.storagePermissionGranted);
        setPermissionChipColor(batteryGranted, bnd.batteryPermissionGranted);
        setPermissionChipColor(notificationsGranted, bnd.notificationPermissionGranted);
        setPermissionChipColor(autorevokeGranted, bnd.ignoreAutorevokeGranted);
        
        bnd.grantPermissionsButton.setVisibility(allGranted ? View.GONE : View.VISIBLE);
        bnd.allSetText.setVisibility(allGranted ? View.VISIBLE : View.GONE);
    }
    
    private static void setPermissionChipColor(boolean permissionGranted, @NonNull TextView permissionText) {
        permissionText.setText(permissionGranted ? R.string.permissions_granted : R.string.permissions_not_granted);
        
        int containerColor = MaterialColors.getColor(permissionText, R.attr.colorErrorContainer, Color.LTGRAY);
        int onContainerColor = MaterialColors.getColor(permissionText, R.attr.colorOnErrorContainer, Color.RED);
        
        if (permissionGranted) {
            containerColor = GraphicsUtilities.swapRedAndGreenChannels(containerColor);
            onContainerColor = GraphicsUtilities.swapRedAndGreenChannels(onContainerColor);
        }
        
        permissionText.setTextColor(onContainerColor);
        permissionText.setBackgroundTintList(ColorStateList.valueOf(containerColor));
    }
    
    @Override
    public boolean isPolicyRespected() {
        return areEssentialPermissionsGranted(requireContext());
    }
    
    @Override
    public void onUserIllegallyRequestedNextPage() {
        displayGrantPermissionsToast();
    }
    
    private void displayGrantPermissionsToast() {
        displayToast(requireContext(), R.string.permission_request_description);
    }
}
