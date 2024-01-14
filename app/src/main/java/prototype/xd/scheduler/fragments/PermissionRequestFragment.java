package prototype.xd.scheduler.fragments;

import static prototype.xd.scheduler.fragments.PermissionRequestFragment.PermissionsRequestChainStatus.AUTOREVOKE_PERMISSIONS;
import static prototype.xd.scheduler.fragments.PermissionRequestFragment.PermissionsRequestChainStatus.BATTERY_PERMISSIONS;
import static prototype.xd.scheduler.fragments.PermissionRequestFragment.PermissionsRequestChainStatus.CORE_PERMISSIONS;
import static prototype.xd.scheduler.fragments.PermissionRequestFragment.PermissionsRequestChainStatus.END;
import static prototype.xd.scheduler.utilities.ImageUtilities.getOnBgColor;
import static prototype.xd.scheduler.utilities.PermissionUtilities.AutoRevokeStatus;
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
import prototype.xd.scheduler.utilities.ImageUtilities;
import prototype.xd.scheduler.utilities.Logger;


public final class PermissionRequestFragment extends BaseFragment<PermissionsRequestFragmentBinding> implements SlidePolicy { // NOSONAR this is a fragment
    
    public static final String NAME = PermissionRequestFragment.class.getSimpleName();
    
    enum PermissionsRequestChainStatus {
        CORE_PERMISSIONS, BATTERY_PERMISSIONS, AUTOREVOKE_PERMISSIONS, END
    }
    
    private final ActivityResultLauncher<Intent> intentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> onActivityResult());
    
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), r -> onActivityResult());
    
    @Override
    @NonNull
    public PermissionsRequestFragmentBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return PermissionsRequestFragmentBinding.inflate(inflater, container, false);
    }
    
    private PermissionsRequestChainStatus requestChainStatus = CORE_PERMISSIONS;
    
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
        
        if (getAutorevokeStatus(wrapper) == AutoRevokeStatus.NOT_AVAILABLE) {
            binding.ignoreAutorevokeIcon.setVisibility(View.GONE);
            binding.ignoreAutorevokeTitle.setVisibility(View.GONE);
            binding.ignoreAutorevokeDescription.setVisibility(View.GONE);
            binding.ignoreAutorevokeGranted.setVisibility(View.GONE);
        }
        
        binding.grantPermissionsButton.setOnClickListener(v -> requestPermissions());
    }
    
    private void onActivityResult() {
        displayPermissions();
        requestPermissions();
    }
    
    @SuppressLint("BatteryLife")
    private void requestPermissions() {
        // TODO: shouldShowRequestPermissionRationale()
        boolean essentialsGranted = areEssentialPermissionsGranted(wrapper.context);
        
        switch (requestChainStatus) {
            case CORE_PERMISSIONS:
                requestChainStatus = BATTERY_PERMISSIONS;
                if (essentialsGranted) {
                    // request battery permissions next
                    Logger.info(NAME, "Core already granted, skipping");
                    requestPermissions();
                } else {
                    Logger.info(NAME, "Requesting core permissions");
                    requestPermissionLauncher.launch(getPermissions());
                }
                return;
            
            case BATTERY_PERMISSIONS:
                if (!essentialsGranted) {
                    Logger.info(NAME, "Asking to re-request core permissions");
                    requestChainStatus = CORE_PERMISSIONS;
                    displayGrantPermissionsToast();
                    return;
                }
                requestChainStatus = AUTOREVOKE_PERMISSIONS;
                if (isBatteryGranted(wrapper)) {
                    Logger.info(NAME, "Battery already granted, skipping");
                    requestPermissions();
                } else {
                    Logger.info(NAME, "Requesting battery permissions");
                    intentLauncher.launch(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + PACKAGE_NAME)));
                }
                return;
            
            case AUTOREVOKE_PERMISSIONS:
                requestChainStatus = END;
                if (isAutorevokeGranted(wrapper)) {
                    Logger.info(NAME, "Autorevoke already granted, skipping");
                    requestPermissions();
                } else {
                    Logger.info(NAME, "Requesting autorevoke permissions");
                    displayToast(wrapper.context, R.string.autorevoke_request_description);
                    intentLauncher.launch(IntentCompat.createManageUnusedAppRestrictionsIntent(wrapper.context, PACKAGE_NAME));
                }
                return;
            case END:
                Logger.info(NAME, "Request chain ended");
                requestChainStatus = CORE_PERMISSIONS;
        }
    }
    
    private void displayPermissions() {
        
        boolean notificationsGranted = isNotificationGranted(wrapper);
        
        boolean batteryGranted = isBatteryGranted(wrapper);
        boolean storageGranted = isStorageGranted(wrapper.context);
        boolean calendarGranted = isCalendarGranted(wrapper.context);
        AutoRevokeStatus autorevokeStatus = getAutorevokeStatus(wrapper);
        
        boolean allGranted = storageGranted && calendarGranted &&
                batteryGranted && notificationsGranted &&
                (autorevokeStatus == AutoRevokeStatus.DISABLED);
        
        setPermissionChipColor(calendarGranted, binding.calendarPermissionGranted);
        setPermissionChipColor(storageGranted, binding.storagePermissionGranted);
        setPermissionChipColor(batteryGranted, binding.batteryPermissionGranted);
        setPermissionChipColor(notificationsGranted, binding.notificationPermissionGranted);
        
        if (autorevokeStatus == AutoRevokeStatus.AVAILABLE) {
            int yellow = wrapper.getColor(R.color.yellow_harmonized);
            binding.ignoreAutorevokeGranted.setTextColor(getOnBgColor(yellow));
            binding.ignoreAutorevokeGranted.setBackgroundTintList(ColorStateList.valueOf(yellow));
            binding.ignoreAutorevokeGranted.setText(R.string.permission_state_unknown);
        } else {
            setPermissionChipColor(autorevokeStatus == AutoRevokeStatus.DISABLED, binding.ignoreAutorevokeGranted);
        }
        
        binding.grantPermissionsButton.setVisibility(allGranted ? View.GONE : View.VISIBLE);
        binding.allSetText.setVisibility(allGranted ? View.VISIBLE : View.GONE);
    }
    
    private static void setPermissionChipColor(boolean permissionGranted, @NonNull TextView permissionText) {
        permissionText.setText(permissionGranted ? R.string.permission_granted : R.string.permission_not_granted);
        
        int containerColor = MaterialColors.getColor(permissionText, R.attr.colorErrorContainer, Color.LTGRAY);
        int onContainerColor = MaterialColors.getColor(permissionText, R.attr.colorOnErrorContainer, Color.RED);
        
        if (permissionGranted) {
            containerColor = ImageUtilities.swapRedAndGreenChannels(containerColor);
            onContainerColor = ImageUtilities.swapRedAndGreenChannels(onContainerColor);
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
