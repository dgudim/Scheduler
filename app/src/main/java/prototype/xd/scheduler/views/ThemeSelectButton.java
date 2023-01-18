package prototype.xd.scheduler.views;

import static prototype.xd.scheduler.utilities.Keys.APP_THEME_DARK;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_LIGHT;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_SYSTEM;
import static prototype.xd.scheduler.utilities.Keys.DEFAULT_APP_THEME;
import static prototype.xd.scheduler.utilities.Keys.APP_THEMES;

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
        AtomicReference<Integer> themeId = new AtomicReference<>(DEFAULT_APP_THEME);
        
        if (!isInEditMode()) {
            themeId.set(Keys.APP_THEME.get());
            themeIndex.set(APP_THEMES.indexOf(themeId.get()));
            
            setOnClickListener(v -> {
                themeIndex.set((themeIndex.get() + 1) % APP_THEMES.size());
                themeId.set(APP_THEMES.get(themeIndex.get()));
                AppCompatDelegate.setDefaultNightMode(themeId.get());
                Keys.APP_THEME.put(themeId.get());
                updateThemeIcon(themeId.get());
            });
        }
        
        updateThemeIcon(themeId.get());
    }
    
    private void updateThemeIcon(int themeId) {
        switch (themeId) {
            case APP_THEME_DARK:
                setIconResource(R.drawable.ic_theme_dark_30);
                setText(getContext().getString(R.string.app_theme_dark));
                break;
            case APP_THEME_LIGHT:
                setIconResource(R.drawable.ic_theme_light_30);
                setText(getContext().getString(R.string.app_theme_light));
                break;
            case APP_THEME_SYSTEM:
            default:
                setIconResource(R.drawable.ic_theme_auto_30);
                setText(getContext().getString(R.string.app_theme_system));
                break;
        }
    }
}
