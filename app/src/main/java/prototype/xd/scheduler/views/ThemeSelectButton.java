package prototype.xd.scheduler.views;

import static prototype.xd.scheduler.utilities.Keys.APP_THEME_DARK;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_LIGHT;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_SYSTEM;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_APP_THEME;
import static prototype.xd.scheduler.utilities.Keys.appThemes;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

import java.util.concurrent.atomic.AtomicReference;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;

public class ThemeSelectButton extends MaterialButton {
    
    public ThemeSelectButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        
        AtomicReference<Integer> themeIndex = new AtomicReference<>(0);
        AtomicReference<Byte> themeId = new AtomicReference<>(DEFAULT_APP_THEME);
        
        if (!isInEditMode()) {
            themeId.set((byte) preferences.getInt(Keys.APP_THEME, DEFAULT_APP_THEME));
            themeIndex.set(appThemes.indexOf(themeId.get()));
            
            setOnClickListener(v -> {
                themeIndex.set((themeIndex.get() + 1) % appThemes.size());
                themeId.set(appThemes.get(themeIndex.get()));
                AppCompatDelegate.setDefaultNightMode(themeId.get());
                preferences.edit().putInt(Keys.APP_THEME, themeId.get()).apply();
                updateThemeIcon(themeId.get());
            });
        }
        
        updateThemeIcon(themeId.get());
    }
    
    private void updateThemeIcon(byte themeId) {
        switch (themeId) {
            case APP_THEME_DARK:
                setIconResource(R.drawable.ic_theme_dark);
                setText(getContext().getString(R.string.app_theme_dark));
                break;
            case APP_THEME_LIGHT:
                setIconResource(R.drawable.ic_theme_light);
                setText(getContext().getString(R.string.app_theme_light));
                break;
            case APP_THEME_SYSTEM:
            default:
                setIconResource(R.drawable.ic_theme_auto);
                setText(getContext().getString(R.string.app_theme_system));
                break;
        }
    }
}
