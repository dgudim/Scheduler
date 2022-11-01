package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.APP_THEME_SELECTOR;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_DARK;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_LIGHT;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_SYSTEM;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.views.Spinner;

public class AppThemeSelectorEntry extends SettingsEntry {
    
    private ArrayAdapter<String> spinnerAdapter;
    
    public AppThemeSelectorEntry() {
        super(R.layout.settings_app_theme_selector_entry);
        entryType = APP_THEME_SELECTOR;
    }
    
    @Override
    protected View initInnerViews(View convertView, ViewGroup viewGroup) {
        if (spinnerAdapter == null) {
            List<String> themeNames = new ArrayList<>();
            Context context = convertView.getContext();
            themeNames.add(context.getString(R.string.app_theme_light));
            themeNames.add(context.getString(R.string.app_theme_dark));
            themeNames.add(context.getString(R.string.app_theme_system));
            spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, themeNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }
        Spinner spinnerView = convertView.findViewById(R.id.themeSpinner);
        spinnerView.setAdapter(spinnerAdapter);
        switch (preferences.getInt(Keys.APP_THEME, Keys.DEFAULT_APP_THEME)) {
            case APP_THEME_LIGHT:
                spinnerView.setSelectionSilent(0);
                break;
            case APP_THEME_DARK:
                spinnerView.setSelectionSilent(1);
                break;
            case APP_THEME_SYSTEM:
                spinnerView.setSelectionSilent(2);
                break;
        }
        
        spinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int theme = APP_THEME_SYSTEM;
                if (position == 0) {
                    theme = APP_THEME_LIGHT;
                } else if (position == 1) {
                    theme = APP_THEME_DARK;
                }
                preferences.edit().putInt(Keys.APP_THEME, theme).apply();
                AppCompatDelegate.setDefaultNightMode(theme);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //ignore
            }
        });
        return super.initInnerViews(convertView, viewGroup);
    }
}
