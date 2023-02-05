package prototype.xd.scheduler.fragments;

import static android.app.Activity.RESULT_CANCELED;
import static androidx.core.content.UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE;
import static prototype.xd.scheduler.utilities.PermissionUtilities.areEssentialPermissionsGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.getAutorevokeStatus;
import static prototype.xd.scheduler.utilities.PermissionUtilities.getPermissions;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isAutorevokeGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isBatteryGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isCalendarGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isNotificationGranted;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isOnAndroid13OrHigher;
import static prototype.xd.scheduler.utilities.PermissionUtilities.isStorageGranted;
import static prototype.xd.scheduler.utilities.Static.PACKAGE_NAME;
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

import com.github.appintro.SlidePolicy;
import com.google.android.material.color.MaterialColors;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.PermissionsRequestFragmentBinding;
import prototype.xd.scheduler.utilities.ColorUtilities;


public class PermissionRequestFragment extends BaseFragment<PermissionsRequestFragmentBinding> implements SlidePolicy { // NOSONAR this is a fragment
    
    private final ActivityResultLauncher<Intent> intentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result ->
                    onActivityResult(result.getResultCode() == RESULT_CANCELED));
    
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result ->
                    onActivityResult(result.containsValue(Boolean.FALSE))
    );
    
    @Override
    @NonNull
    public PermissionsRequestFragmentBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return PermissionsRequestFragmentBinding.inflate(inflater, container, false);
    }
    
    @Override
    @MainThread
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        
        displayPermissions();
        
        if (!isOnAndroid13OrHigher()) {
            binding.notificationPermissionIcon.setVisibility(View.GONE);
            binding.notificationPermissionTitle.setVisibility(View.GONE);
            binding.notificationPermissionDescription.setVisibility(View.GONE);
            binding.notificationPermissionGranted.setVisibility(View.GONE);
        }
        
        if (getAutorevokeStatus(this) == FEATURE_NOT_AVAILABLE) {
            binding.ignoreAutorevokeIcon.setVisibility(View.GONE);
            binding.ignoreAutorevokeTitle.setVisibility(View.GONE);
            binding.ignoreAutorevokeDescription.setVisibility(View.GONE);
            binding.ignoreAutorevokeGranted.setVisibility(View.GONE);
        }
        
        binding.grantPermissionsButton.setOnClickListener(v -> requestPermissions(false));
    }
    
    private void onActivityResult(boolean userRejected) {
        displayPermissions();
        requestPermissions(userRejected);
    }
    
    @SuppressLint("BatteryLife")
    private void requestPermissions(boolean userRejected) {
        
        boolean storageGranted = isStorageGranted(wrapper.context);
        boolean calendarGranted = isCalendarGranted(wrapper.context);
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
            displayToast(wrapper.context, R.string.autorevoke_request_description);
            intentLauncher.launch(IntentCompat.createManageUnusedAppRestrictionsIntent(wrapper.context, PACKAGE_NAME));
        }
    }
    
    private void displayPermissions() {
        
        boolean notificationsGranted = isNotificationGranted(this);
        
        boolean batteryGranted = isBatteryGranted(this);
        boolean storageGranted = isStorageGranted(wrapper.context);
        boolean calendarGranted = isCalendarGranted(wrapper.context);
        boolean autorevokeGranted = isAutorevokeGranted(this);
        
        boolean allGranted = storageGranted && calendarGranted && batteryGranted && notificationsGranted && autorevokeGranted;
        
        setPermissionChipColor(calendarGranted, binding.calendarPermissionGranted);
        setPermissionChipColor(storageGranted, binding.storagePermissionGranted);
        setPermissionChipColor(batteryGranted, binding.batteryPermissionGranted);
        setPermissionChipColor(notificationsGranted, binding.notificationPermissionGranted);
        setPermissionChipColor(autorevokeGranted, binding.ignoreAutorevokeGranted);
        
        binding.grantPermissionsButton.setVisibility(allGranted ? View.GONE : View.VISIBLE);
        binding.allSetText.setVisibility(allGranted ? View.VISIBLE : View.GONE);
    }
    
    private static void setPermissionChipColor(boolean permissionGranted, @NonNull TextView permissionText) {
        permissionText.setText(permissionGranted ? R.string.permissions_granted : R.string.permissions_not_granted);
        
        int containerColor = MaterialColors.getColor(permissionText, R.attr.colorErrorContainer, Color.LTGRAY);
        int onContainerColor = MaterialColors.getColor(permissionText, R.attr.colorOnErrorContainer, Color.RED);
        
        if (permissionGranted) {
            containerColor = ColorUtilities.swapRedAndGreenChannels(containerColor);
            onContainerColor = ColorUtilities.swapRedAndGreenChannels(onContainerColor);
        }
        
        permissionText.setTextColor(onContainerColor);
        permissionText.setBackgroundTintList(ColorStateList.valueOf(containerColor));
    }
    
    @Override
    public boolean isPolicyRespected() {
        return areEssentialPermissionsGranted(wrapper.context);
    }
    
    @Override
    public void onUserIllegallyRequestedNextPage() {
        displayGrantPermissionsToast();
    }
    
    private void displayGrantPermissionsToast() {
        displayToast(wrapper.context, R.string.permission_request_description);
    }
}
