package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;

public class SystemCalendarSettings {
    
    ArrayList<String> calendarSubKeys;
    
    TextView fontColor_view_state;
    TextView bgColor_view_state;
    TextView padColor_view_state;
    TextView adaptiveColor_switch_state;
    TextView priority_state;
    TextView padSize_state;
    TextView show_on_lock_state;
    TextView adaptiveColor_bar_state;
    TextView showDaysBeforehand_bar_state;
    TextView showDaysAfter_bar_state;
    
    public SystemCalendarSettings(final Context context, final SettingsFragment fragment, View settingsView, final String calendar_key) {
        
        settingsView.findViewById(R.id.group_selector).setVisibility(View.GONE);
        
        calendarSubKeys = new ArrayList<>();
        String[] key_split = calendar_key.split("_");
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < key_split.length; i++) {
            if (i != 0) {
                buffer.append('_');
            }
            buffer.append(key_split[i]);
            calendarSubKeys.add(buffer.toString());
        }
        
        initialise(calendar_key, context, fragment, settingsView);
        
        new AlertDialog.Builder(context).setView(settingsView).show();
    }
    
    private int getFirstValidKeyIndex(String parameter) {
        for (int i = calendarSubKeys.size() - 1; i >= 0; i--) {
            try {
                if (preferences.getString(calendarSubKeys.get(i) + "_" + parameter, null) != null) {
                    return i;
                }
            } catch (ClassCastException e) {
                return i;
            }
        }
        return -1;
    }
    
    private String getFirstValidKey(String parameter) {
        int index = getFirstValidKeyIndex(parameter);
        return index == -1 ? parameter : calendarSubKeys.get(index) + "_" + parameter;
    }
    
    private void initialise(String calendarKey, final Context context, final SettingsFragment fragment, final View settingsView) {
        
        int fontColor = preferences.getInt(getFirstValidKey(Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
        int bgColor = preferences.getInt(getFirstValidKey(Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR);
        int bevelColor = preferences.getInt(getFirstValidKey(Keys.BEVEL_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR);
        int bevelThickness = preferences.getInt(getFirstValidKey(Keys.BEVEL_THICKNESS), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS);
        
        int adaptiveColorBalance = preferences.getInt(getFirstValidKey(Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
        boolean adaptiveColorEnabled = preferences.getBoolean(getFirstValidKey(Keys.ADAPTIVE_COLOR_ENABLED), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED);
        
        boolean showOnLock = preferences.getBoolean(getFirstValidKey(Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
        int priority = preferences.getInt(getFirstValidKey(Keys.PRIORITY), Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY);
        
        int dayOffset_right = preferences.getInt(getFirstValidKey(Keys.BEFOREHAND_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_BEFOREHAND_ITEMS_OFFSET);
        int dayOffset_left = preferences.getInt(getFirstValidKey(Keys.AFTER_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_AFTER_ITEMS_OFFSET);
        
        View fontColor_view = settingsView.findViewById(R.id.textColor);
        View bgColor_view = settingsView.findViewById(R.id.backgroundColor);
        View padColor_view = settingsView.findViewById(R.id.bevelColor);
        fontColor_view.setBackgroundColor(fontColor);
        bgColor_view.setBackgroundColor(bgColor);
        padColor_view.setBackgroundColor(bevelColor);
        
        fontColor_view_state = settingsView.findViewById(R.id.font_color_state);
        bgColor_view_state = settingsView.findViewById(R.id.background_color_state);
        padColor_view_state = settingsView.findViewById(R.id.bevel_color_state);
        priority_state = settingsView.findViewById(R.id.priority_state);
        padSize_state = settingsView.findViewById(R.id.bevel_size_state);
        show_on_lock_state = settingsView.findViewById(R.id.show_on_lock_state);
        adaptiveColor_switch_state = settingsView.findViewById(R.id.adaptive_color_state);
        adaptiveColor_bar_state = settingsView.findViewById(R.id.adaptive_color_balance_state);
        showDaysBeforehand_bar_state = settingsView.findViewById(R.id.days_beforehand_state);
        showDaysAfter_bar_state = settingsView.findViewById(R.id.days_after_state);
        
        updateAllIndicators();
        
        settingsView.findViewById(R.id.settingsResetButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Сбросить настройки?");
            
            builder.setPositiveButton("Да", (dialog, which) -> {
                // TODO: 22/1/2022 reset settings
                preferences.edit().putBoolean(Keys.NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                initialise(calendarKey, context, fragment, settingsView);
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        fontColor_view.setOnClickListener(view -> invokeColorDialogue(context,
                view, fontColor_view_state,
                this,
                calendarKey, Keys.FONT_COLOR, fontColor, true));
        
        bgColor_view.setOnClickListener(view -> invokeColorDialogue(context,
                view, bgColor_view_state,
                this,
                calendarKey, Keys.BG_COLOR, bgColor, true));
        
        padColor_view.setOnClickListener(view -> invokeColorDialogue(context,
                view, padColor_view_state,
                this,
                calendarKey, Keys.BEVEL_COLOR, bevelColor, true));
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.bevelThicknessDescription),
                settingsView.findViewById(R.id.bevelThicknessBar),
                padSize_state, this, fragment, R.string.settings_bevel_thickness,
                calendarKey, Keys.BEVEL_THICKNESS, bevelThickness);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.priorityDescription),
                settingsView.findViewById(R.id.priorityBar),
                priority_state, this, fragment, R.string.settings_priority,
                calendarKey, Keys.PRIORITY, priority);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.adaptive_color_balance_description),
                settingsView.findViewById(R.id.adaptive_color_balance_bar),
                adaptiveColor_bar_state, this, fragment, R.string.settings_adaptive_color_balance,
                calendarKey, Keys.ADAPTIVE_COLOR_BALANCE, adaptiveColorBalance);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.show_days_beforehand_description),
                settingsView.findViewById(R.id.show_days_beforehand_bar),
                showDaysBeforehand_bar_state, this, fragment, R.string.settings_show_days_beforehand,
                calendarKey, Keys.BEFOREHAND_ITEMS_OFFSET, dayOffset_right);
        
        addSeekBarChangeListener(
                settingsView.findViewById(R.id.show_days_after_description),
                settingsView.findViewById(R.id.show_days_after_bar),
                showDaysAfter_bar_state, this, fragment, R.string.settings_show_days_after,
                calendarKey, Keys.AFTER_ITEMS_OFFSET, dayOffset_left);
        
        addSwitchChangeListener(
                settingsView.findViewById(R.id.showOnLockSwitch),
                show_on_lock_state, this,
                calendarKey, Keys.SHOW_ON_LOCK, showOnLock);
        
        addSwitchChangeListener(
                settingsView.findViewById(R.id.adaptive_color_switch),
                adaptiveColor_switch_state, this,
                calendarKey, Keys.ADAPTIVE_COLOR_ENABLED, adaptiveColorEnabled);
    }
    
    void setStateIconColor(TextView display, String parameter) {
        int keyIndex = getFirstValidKeyIndex(parameter);
        if (keyIndex == calendarSubKeys.size() - 1) {
            display.setTextColor(Color.GREEN);
        } else if (keyIndex >= 0) {
            display.setTextColor(Color.YELLOW);
        }
    }
    
    void updateAllIndicators() {
        setStateIconColor(fontColor_view_state, Keys.FONT_COLOR);
        setStateIconColor(bgColor_view_state, Keys.BG_COLOR);
        setStateIconColor(padColor_view_state, Keys.BEVEL_COLOR);
        setStateIconColor(padSize_state, Keys.BEVEL_THICKNESS);
        setStateIconColor(priority_state, Keys.PRIORITY);
        setStateIconColor(show_on_lock_state, Keys.SHOW_ON_LOCK);
        setStateIconColor(adaptiveColor_switch_state, Keys.ADAPTIVE_COLOR_ENABLED);
        setStateIconColor(adaptiveColor_bar_state, Keys.ADAPTIVE_COLOR_BALANCE);
        setStateIconColor(showDaysBeforehand_bar_state, Keys.BEFOREHAND_ITEMS_OFFSET);
        setStateIconColor(showDaysAfter_bar_state, Keys.AFTER_ITEMS_OFFSET);
    }
}
