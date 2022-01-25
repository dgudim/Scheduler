package prototype.xd.scheduler;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    
    public static SharedPreferences preferences;
    public static DisplayMetrics displayMetrics;
    
    private TextView calendar_permission_text;
    private TextView storage_permission_text;
    
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CALENDAR
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        if (!refreshPermissionStates()) {
            setContentView(R.layout.permissions_request_screen);
            calendar_permission_text = findViewById(R.id.calendar_permission_granted);
            storage_permission_text = findViewById(R.id.storage_permission_granted);
            refreshPermissionStates();
            View grant_button = findViewById(R.id.grant_permissions_button);
            grant_button.setOnClickListener(v -> requestPermissions(PERMISSIONS, 41));
        } else {
            setContentView(R.layout.activity_main);
        }
    }
    
    private boolean refreshPermissionStates() {
        boolean storage_granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean calendar_granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        
        if (calendar_permission_text != null && storage_permission_text != null) {
            calendar_permission_text.setText(calendar_granted ? R.string.permissions_granted : R.string.permissions_not_granted);
            calendar_permission_text.setTextColor(getColor(calendar_granted ? R.color.color_permission_granted : R.color.color_permission_not_granted));
            
            storage_permission_text.setText(storage_granted ? R.string.permissions_granted : R.string.permissions_not_granted);
            storage_permission_text.setTextColor(getColor(storage_granted ? R.color.color_permission_granted : R.color.color_permission_not_granted));
        }
        
        return storage_granted && calendar_granted;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (refreshPermissionStates()) {
            setContentView(R.layout.activity_main);
        }
    }
}