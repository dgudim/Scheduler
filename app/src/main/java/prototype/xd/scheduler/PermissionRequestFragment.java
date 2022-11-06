package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.PACKAGE_NAME;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.appintro.SlidePolicy;
import com.google.android.material.color.MaterialColors;


public class PermissionRequestFragment extends Fragment implements SlidePolicy {
    
    private View rootView;
    
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CALENDAR
    };
    
    @SuppressLint("BatteryLife")
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (!refreshPermissionStates(true)) {
                    displayGrantToast();
                } else {
                    startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + PACKAGE_NAME)));
                }
            }
    );
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.permissions_request_fragment, container, false);
        refreshPermissionStates(true);
        rootView.findViewById(R.id.grant_permissions_button).setOnClickListener(v -> requestPermissionLauncher.launch(PERMISSIONS));
        return rootView;
    }
    
    private boolean refreshPermissionStates(boolean display) {
        boolean storageGranted = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean calendarGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        boolean batteryGranted = ((PowerManager) requireActivity().getSystemService(Context.POWER_SERVICE))
                .isIgnoringBatteryOptimizations(PACKAGE_NAME);
        
        boolean essentialGranted = storageGranted && calendarGranted;
        boolean allGranted = essentialGranted && batteryGranted;
        
        if (display) {
            setPermissionChipColor(calendarGranted,
                    rootView.findViewById(R.id.calendar_permission_granted),
                    rootView.findViewById(R.id.calendar_permission_granted_bg));
            
            setPermissionChipColor(storageGranted,
                    rootView.findViewById(R.id.storage_permission_granted),
                    rootView.findViewById(R.id.storage_permission_granted_bg));
            
            setPermissionChipColor(batteryGranted,
                    rootView.findViewById(R.id.battery_permission_granted),
                    rootView.findViewById(R.id.battery_permission_granted_bg));
            
            rootView.findViewById(R.id.grant_permissions_button).setVisibility(allGranted ? View.GONE : View.VISIBLE);
            rootView.findViewById(R.id.all_set_text).setVisibility(allGranted ? View.VISIBLE : View.GONE);
        }
        
        return essentialGranted;
    }
    
    private void setPermissionChipColor(boolean permissionGranted, TextView permissionText, CardView permissionTextBg) {
        permissionText.setText(permissionGranted ? R.string.permissions_granted : R.string.permissions_not_granted);
        permissionText.setTextColor(MaterialColors.getColor(permissionText, permissionGranted ? R.attr.colorOnTertiaryContainer : R.attr.colorOnErrorContainer,
                permissionGranted ? Color.GREEN : Color.RED));
        permissionTextBg.setCardBackgroundColor(MaterialColors.getColor(permissionTextBg, permissionGranted ? R.attr.colorTertiaryContainer : R.attr.colorErrorContainer,
                Color.LTGRAY));
    }
    
    @Override
    public boolean isPolicyRespected() {
        return refreshPermissionStates(false);
    }
    
    @Override
    public void onUserIllegallyRequestedNextPage() {
        displayGrantToast();
    }
    
    private void displayGrantToast() {
        Toast.makeText(getActivity(), requireContext().getString(R.string.permission_request_description),
                Toast.LENGTH_LONG).show();
    }
}
