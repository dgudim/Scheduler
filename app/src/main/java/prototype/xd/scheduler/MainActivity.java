package prototype.xd.scheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.os.Bundle;
import android.view.Menu;
import android.view.WindowInsets;
import android.view.WindowMetrics;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static SharedPreferences preferences;
    public static WindowMetrics windowMetrics;
    public static Insets windowInsets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        windowMetrics = getWindowManager().getCurrentWindowMetrics();
        windowInsets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
}