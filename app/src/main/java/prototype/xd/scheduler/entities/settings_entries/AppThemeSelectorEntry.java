package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.APP_THEME_SELECTOR;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_DARK;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_LIGHT;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_SYSTEM;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;

public class AppThemeSelectorEntry extends SettingsEntry {
    
    public AppThemeSelectorEntry() {
        super(R.layout.settings_app_theme_selector_entry);
        entryType = APP_THEME_SELECTOR;
    }
    
    private final List<Byte> themes = Arrays.asList(APP_THEME_DARK, APP_THEME_SYSTEM, APP_THEME_LIGHT);
    
    private void updateThemeIcon(ImageButton themeButton, byte themeId) {
        switch (themeId) {
            case APP_THEME_DARK:
                themeButton.setImageResource(R.drawable.ic_theme_dark);
                break;
            case APP_THEME_LIGHT:
                themeButton.setImageResource(R.drawable.ic_theme_light);
                break;
            case APP_THEME_SYSTEM:
            default:
                themeButton.setImageResource(R.drawable.ic_theme_auto);
                break;
        }
    }
    
    @Override
    protected View initInnerViews(View convertView, ViewGroup viewGroup) {
    
        AtomicReference<Integer> themeIndex = new AtomicReference<>(0);
        AtomicReference<Byte> themeId = new AtomicReference<>((byte) 0);
        
        ImageButton themeButton = convertView.findViewById(R.id.theme_switch_button);
        themeId.set((byte) preferences.getInt(Keys.APP_THEME, Keys.DEFAULT_APP_THEME));
        themeIndex.set(themes.indexOf(themeId.get()));
        updateThemeIcon(themeButton, themeId.get());
        
        themeButton.setOnClickListener(v -> {
            themeIndex.set((themeIndex.get() + 1) % themes.size());
            themeId.set(themes.get(themeIndex.get()));
            AppCompatDelegate.setDefaultNightMode(themeId.get());
            preferences.edit().putInt(Keys.APP_THEME, themeId.get()).apply();
            updateThemeIcon(themeButton, themeId.get());
        });
        
        return super.initInnerViews(convertView, viewGroup);
    }
}
