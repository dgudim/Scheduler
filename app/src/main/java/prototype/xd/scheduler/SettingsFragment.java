package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.BackgroundChooser.defaultBackgroundName;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createSolidColorCircle;
import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.File;

import prototype.xd.scheduler.utilities.Keys;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ApplySharedPref"})
public class SettingsFragment extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        view.findViewById(R.id.resetBgButton).setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Удалить все сохраненные фоны?");
            
            builder.setPositiveButton("Да", (dialog, which) -> {
                new File(rootDir, defaultBackgroundName).delete();
                for (int i = 0; i < 7; i++) {
                    new File(rootDir, availableDays[i] + ".png").delete();
                }
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        ImageView todayBgColor = view.findViewById(R.id.defaultColor);
        ImageView todayBevelColor = view.findViewById(R.id.defaultBevelColor);
        ImageView todayFontColor = view.findViewById(R.id.defaultFontColor);
        
        ImageView oldBgColor = view.findViewById(R.id.defaultOldColor);
        ImageView oldBevelColor = view.findViewById(R.id.defaultOldBevelColor);
        ImageView oldFontColor = view.findViewById(R.id.oldFontColor);
        
        ImageView newBgColor = view.findViewById(R.id.newColor);
        ImageView newBevelColor = view.findViewById(R.id.newBevelColor);
        ImageView newFontColor = view.findViewById(R.id.newFontColor);
        
        todayBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.TODAY_BG_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR)));
        oldBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.OLD_BG_COLOR, Keys.SETTINGS_DEFAULT_OLD_BG_COLOR)));
        newBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.NEW_BG_COLOR, Keys.SETTINGS_DEFAULT_NEW_BG_COLOR)));
        
        todayBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.TODAY_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR)));
        oldBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.OLD_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_OLD_BEVEL_COLOR)));
        newBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.NEW_BEVEL_COLOR, Keys.SETTINGS_DEFAULT_NEW_BEVEL_COLOR)));
    
        todayFontColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.TODAY_FONT_COLOR, Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR)));
        oldFontColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.OLD_FONT_COLOR, Keys.SETTINGS_DEFAULT_OLD_FONT_COLOR)));
        newFontColor.setImageBitmap(createSolidColorCircle(preferences.getInt(Keys.NEW_FONT_COLOR, Keys.SETTINGS_DEFAULT_NEW_FONT_COLOR)));
        
        todayBgColor.setOnClickListener(v -> invokeColorDialogue(Keys.TODAY_BG_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR, getContext()));
        oldBgColor.setOnClickListener(v -> invokeColorDialogue(Keys.OLD_BG_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_OLD_BG_COLOR, getContext()));
        newBgColor.setOnClickListener(v -> invokeColorDialogue(Keys.NEW_BG_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_NEW_BG_COLOR, getContext()));
        
        todayBevelColor.setOnClickListener(v -> invokeColorDialogue(Keys.TODAY_BEVEL_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR, getContext()));
        oldBevelColor.setOnClickListener(v -> invokeColorDialogue(Keys.OLD_BEVEL_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_OLD_BEVEL_COLOR, getContext()));
        newBevelColor.setOnClickListener(v -> invokeColorDialogue(Keys.NEW_BEVEL_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_NEW_BEVEL_COLOR, getContext()));
    
        todayFontColor.setOnClickListener(v -> invokeColorDialogue(Keys.TODAY_FONT_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR, getContext()));
        oldFontColor.setOnClickListener(v -> invokeColorDialogue(Keys.OLD_FONT_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_OLD_FONT_COLOR, getContext()));
        newFontColor.setOnClickListener(v -> invokeColorDialogue(Keys.NEW_FONT_COLOR, (ImageView) v, Keys.SETTINGS_DEFAULT_NEW_FONT_COLOR, getContext()));
        
        addSeekBarChangeListener(
                view.findViewById(R.id.textSizeDescripton),
                view.findViewById(R.id.fontSizeBar),
                Keys.FONT_SIZE, Keys.SETTINGS_DEFAULT_FONT_SIZE, R.string.settings_font_size, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.defaultBevelThickness),
                view.findViewById(R.id.defaultBevelThicknessBar),
                Keys.TODAY_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS, R.string.settings_default_bevel_thickness, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.oldBevelThickness),
                view.findViewById(R.id.oldBevelThicknessBar),
                Keys.OLD_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_OLD_BEVEL_THICKNESS, R.string.settings_old_bevel_thickness, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.newBevelThickness),
                view.findViewById(R.id.newBevelThicknessBar),
                Keys.NEW_BEVEL_THICKNESS, Keys.SETTINGS_DEFAULT_NEW_BEVEL_THICKNESS, R.string.settings_new_bevel_thickness, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.adaptive_color_balance_description),
                view.findViewById(R.id.adaptive_color_balance_bar),
                Keys.ADAPTIVE_COLOR_BALANCE, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE, R.string.settings_adaptive_color_balance, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.show_days_beforehand_description),
                view.findViewById(R.id.show_days_beforehand_bar),
                Keys.NEW_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_NEW_ITEMS_OFFSET, R.string.settings_show_days_beforehand, this);
        
        addSeekBarChangeListener(
                view.findViewById(R.id.show_days_after_description),
                view.findViewById(R.id.show_days_after_bar),
                Keys.OLD_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_OLD_ITEMS_OFFSET, R.string.settings_show_days_after, this);
        
        addSwitchChangeListener(view.findViewById(R.id.show_old_done_tasks_switch), Keys.SHOW_OLD_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_OLD_COMPLETED_ITEMS_IN_LIST, null);
        addSwitchChangeListener(view.findViewById(R.id.show_new_done_tasks_switch), Keys.SHOW_NEW_COMPLETED_ITEMS_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_NEW_COMPLETED_ITEMS_IN_LIST, null);
        addSwitchChangeListener(view.findViewById(R.id.show_global_Items_lock_switch), Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_OLD_COMPLETED_ITEMS_IN_LIST, null);
        addSwitchChangeListener(view.findViewById(R.id.item_full_width_switch), Keys.ITEM_FULL_WIDTH_LOCK, Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK, null);
        addSwitchChangeListener(view.findViewById(R.id.adaptive_color_switch), Keys.ADAPTIVE_COLOR_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED, null);
        addSwitchChangeListener(view.findViewById(R.id.adaptive_background_switch), Keys.ADAPTIVE_BACKGROUND_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_BACKGROUND_ENABLED, null);
        
        view.findViewById(R.id.resetSettingsButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Сбросить настройки?");
            
            builder.setPositiveButton("Да", (dialog, which) -> {
                preferences.edit().clear().commit();
                SettingsFragment.this.onViewCreated(view, savedInstanceState);
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
    }
}