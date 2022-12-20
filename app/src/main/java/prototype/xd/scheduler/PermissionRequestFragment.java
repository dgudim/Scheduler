package prototype.xd.scheduler;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;
import static prototype.xd.scheduler.MainActivity.PACKAGE_NAME;
import static prototype.xd.scheduler.utilities.Utilities.displayToast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.appintro.SlidePolicy;
import com.google.android.material.color.MaterialColors;

import prototype.xd.scheduler.databinding.PermissionsRequestFragmentBinding;


public class PermissionRequestFragment extends Fragment implements SlidePolicy {
    
    private PermissionsRequestFragmentBinding bnd;
    
    ActivityResultLauncher<Intent> batteryOptimizationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> refreshPermissionStates(true));
    
    @SuppressLint("BatteryLife")
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (!refreshPermissionStates(true)) {
                    displayGrantPermissionsToast();
                } else {
                    batteryOptimizationLauncher.launch(
                            new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + PACKAGE_NAME)));
                }
            }
    );
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bnd = PermissionsRequestFragmentBinding.inflate(inflater, container, false);
        refreshPermissionStates(true);
        
        // android 13 and higher
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            // new granular permissions for android 13
            bnd.grantPermissionsButton.setOnClickListener(v -> requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.POST_NOTIFICATIONS}));
        } else {
            // normal permissions
            bnd.grantPermissionsButton.setOnClickListener(v -> requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CALENDAR}));
        }
        
        return bnd.getRoot();
    }
    
    private boolean refreshPermissionStates(boolean display) {

        boolean storageGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        
        boolean calendarGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
        
        boolean notificationsGranted = true;
        // android 13 and higher
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            notificationsGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        
        boolean batteryGranted = ((PowerManager) requireActivity().getSystemService(Context.POWER_SERVICE))
                .isIgnoringBatteryOptimizations(PACKAGE_NAME);
        
        boolean essentialGranted = storageGranted && calendarGranted;
        boolean allGranted = essentialGranted && batteryGranted && notificationsGranted;
        
        if (display) {
            setPermissionChipColor(calendarGranted, bnd.calendarPermissionGranted);
            setPermissionChipColor(storageGranted, bnd.storagePermissionGranted);
            setPermissionChipColor(batteryGranted, bnd.batteryPermissionGranted);
            setPermissionChipColor(notificationsGranted, bnd.notificationPermissionGranted);
            
            bnd.grantPermissionsButton.setVisibility(allGranted ? View.GONE : View.VISIBLE);
            bnd.allSetText.setVisibility(allGranted ? View.VISIBLE : View.GONE);
        }
        
        return essentialGranted;
    }
    
    private void setPermissionChipColor(boolean permissionGranted, TextView permissionText) {
        permissionText.setText(permissionGranted ? R.string.permissions_granted : R.string.permissions_not_granted);
        permissionText.setTextColor(MaterialColors.getColor(permissionText,
                permissionGranted ? R.attr.colorOnSecondaryContainer : R.attr.colorOnErrorContainer,
                permissionGranted ? Color.GREEN : Color.RED));
        permissionText.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(permissionText,
                permissionGranted ? R.attr.colorSecondaryContainer : R.attr.colorErrorContainer,
                Color.LTGRAY)));
    }
    
    @Override
    public boolean isPolicyRespected() {
        return refreshPermissionStates(false);
    }
    
    @Override
    public void onUserIllegallyRequestedNextPage() {
        displayGrantPermissionsToast();
    }
    
    private void displayGrantPermissionsToast() {
        displayToast(requireContext(), R.string.permission_request_description);
    }
}
