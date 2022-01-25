package prototype.xd.scheduler;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    
    public static SharedPreferences preferences;
    public static DisplayMetrics displayMetrics;
    
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SET_WALLPAPER,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        verifyPermissions(this);
        setContentView(R.layout.activity_main);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    void verifyPermissions(Activity activity) {
        boolean granted = false;
        ActivityCompat.requestPermissions(activity, PERMISSIONS, 1);
        while (!granted) {
            granted = true;
            for (String permission : PERMISSIONS) {
                granted = granted && ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
            }
        }
    }
}