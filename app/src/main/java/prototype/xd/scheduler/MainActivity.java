package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.BitmapUtilities.fingerPrintAndSaveBitmap;
import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.initStorage;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    
    public static SharedPreferences preferences;
    
    private TextView calendar_permission_text;
    private TextView storage_permission_text;
    
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CALENDAR
    };
    
    public static final int REQUEST_CODE_PERMISSIONS = 13;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
       /* try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }*/
        
        if (!refreshPermissionStates()) {
            setContentView(R.layout.permissions_request_screen);
            calendar_permission_text = findViewById(R.id.calendar_permission_granted);
            storage_permission_text = findViewById(R.id.storage_permission_granted);
            refreshPermissionStates();
            View grant_button = findViewById(R.id.grant_permissions_button);
            grant_button.setOnClickListener(v -> requestPermissions(PERMISSIONS, REQUEST_CODE_PERMISSIONS));
        } else {
            initStorage(this);
            BackgroundSetterService.restart(this);
            setContentView(R.layout.activity_main);
        }
    }
    
    public void notifyService(){
        new Thread(() -> BackgroundSetterService.ping(MainActivity.this)).start();
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode != RESULT_OK) return;
        if (requestCode >= 0 && requestCode <= 7) {
            Uri uri = intent.getData();
            if (uri != null) {
                new Thread(() -> {
                    try {
                        Bitmap originalBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
    
                        WindowManager wm = (WindowManager) MainActivity.this.getSystemService(Context.WINDOW_SERVICE);
                        Display display = wm.getDefaultDisplay();
    
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        display.getRealMetrics(displayMetrics);
                        
                        Bitmap fingerprintedBitmap = fingerPrintAndSaveBitmap(
                                originalBitmap.copy(Bitmap.Config.ARGB_8888, true),
                                new File(rootDir, availableDays[requestCode] + ".png"),
                                displayMetrics);
                        
                        originalBitmap.recycle();
                        fingerprintedBitmap.recycle();
                        runOnUiThread(() -> ((SettingsFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment))
                                .getChildFragmentManager().getFragments().get(0)).notifyBgSelected());
                    } catch (Exception e) {
                        logException(e);
                    }
                }).start();
            }
        }
    }
}