package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.Keys.APP_THEME_DARK;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_LIGHT;
import static prototype.xd.scheduler.utilities.Keys.APP_THEME_SYSTEM;
import static prototype.xd.scheduler.utilities.Keys.appThemes;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import java.util.concurrent.atomic.AtomicReference;

import prototype.xd.scheduler.utilities.Keys;

public class IntroStartingFragment extends Fragment {
    
    private void updateThemeIcon(ImageButton themeButton, TextView themeText, byte themeId) {
        switch (themeId) {
            case APP_THEME_DARK:
                themeButton.setImageResource(R.drawable.ic_theme_dark);
                themeText.setText(themeText.getContext().getString(R.string.app_theme_dark));
                break;
            case APP_THEME_LIGHT:
                themeButton.setImageResource(R.drawable.ic_theme_light);
                themeText.setText(themeText.getContext().getString(R.string.app_theme_light));
                break;
            case APP_THEME_SYSTEM:
            default:
                themeButton.setImageResource(R.drawable.ic_theme_auto);
                themeText.setText(themeText.getContext().getString(R.string.app_theme_system));
                break;
        }
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.intro_starting_fragment, container, false);
    
        AtomicReference<Integer> themeIndex = new AtomicReference<>(0);
        AtomicReference<Byte> themeId = new AtomicReference<>((byte) 0);
        
        themeId.set((byte) preferences.getInt(Keys.APP_THEME, Keys.DEFAULT_APP_THEME));
        themeIndex.set(appThemes.indexOf(themeId.get()));
        
        TextView themeText = view.findViewById(R.id.app_theme_text);
        ImageButton themeButton = view.findViewById(R.id.app_theme_button);
    
        updateThemeIcon(themeButton, themeText, themeId.get());
    
        View.OnClickListener onClickListener = v -> {
            themeIndex.set((themeIndex.get() + 1) % appThemes.size());
            themeId.set(appThemes.get(themeIndex.get()));
            AppCompatDelegate.setDefaultNightMode(themeId.get());
            preferences.edit().putInt(Keys.APP_THEME, themeId.get()).apply();
            updateThemeIcon(themeButton, themeText, themeId.get());
        };
        
        themeText.setOnClickListener(onClickListener);
        themeButton.setOnClickListener(onClickListener);
        
        return view;
    }
    
}
