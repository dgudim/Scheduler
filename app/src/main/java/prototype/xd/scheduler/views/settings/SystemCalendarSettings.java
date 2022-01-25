package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKeyIndex;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.utilities.Keys;

public class SystemCalendarSettings extends PopupSettingsView{
    
    private final ArrayList<String> calendarSubKeys;
    
    public SystemCalendarSettings(final Context context, final SettingsFragment fragment, View settingsView, final String calendar_key) {
        super(settingsView);
    
        settingsView.findViewById(R.id.group_selector).setVisibility(View.GONE);
        
        calendarSubKeys = generateSubKeysFromKey(calendar_key);
        
        initialise(calendar_key, context, fragment);
        
        new AlertDialog.Builder(context).setView(settingsView).show();
    }
    
    private void initialise(String calendarKey, final Context context, final SettingsFragment fragment) {
        
        int fontColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR);
        int bgColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR);
        int bevelColor = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEVEL_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR);
        int bevelThickness = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEVEL_THICKNESS), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS);
        
        int adaptiveColorBalance = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
        boolean adaptiveColorEnabled = preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_ENABLED), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED);
        
        boolean showOnLock = preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
        int priority = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.PRIORITY), Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY);
        
        int dayOffset_beforehand = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEFOREHAND_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_BEFOREHAND_ITEMS_OFFSET);
        int dayOffset_after = preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.AFTER_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_AFTER_ITEMS_OFFSET);
        
        fontColor_view.setBackgroundColor(fontColor);
        bgColor_view.setBackgroundColor(bgColor);
        padColor_view.setBackgroundColor(bevelColor);
        
        updateAllIndicators();
        
        settings_reset_button.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Сбросить настройки?");
            
            builder.setPositiveButton("Да", (dialog, which) -> {
                Map<String, ?> allEntries = preferences.getAll();
                SharedPreferences.Editor editor = preferences.edit();
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    if (entry.getKey().startsWith(calendarKey)) {
                        editor.remove(entry.getKey());
                    }
                }
                editor.apply();
                initialise(calendarKey, context, fragment);
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
                bevel_thickness_description,
                bevel_thickness_bar,
                padSize_state, this, fragment, R.string.settings_bevel_thickness,
                calendarKey, Keys.BEVEL_THICKNESS, bevelThickness);
        
        addSeekBarChangeListener(
                priority_description,
                priority_bar,
                priority_state, this, fragment, R.string.settings_priority,
                calendarKey, Keys.PRIORITY, priority);
        
        addSeekBarChangeListener(
                adaptive_color_balance_description,
                adaptive_color_balance_bar,
                adaptiveColor_bar_state, this, fragment, R.string.settings_adaptive_color_balance,
                calendarKey, Keys.ADAPTIVE_COLOR_BALANCE, adaptiveColorBalance);
        
        addSeekBarChangeListener(
                show_days_beforehand_description,
                show_days_beforehand_bar,
                showDaysBeforehand_bar_state, this, fragment, R.string.settings_show_days_beforehand,
                calendarKey, Keys.BEFOREHAND_ITEMS_OFFSET, dayOffset_beforehand);
        
        addSeekBarChangeListener(
                show_days_after_description,
                show_days_after_bar,
                showDaysAfter_bar_state, this, fragment, R.string.settings_show_days_after,
                calendarKey, Keys.AFTER_ITEMS_OFFSET, dayOffset_after);
        
        addSwitchChangeListener(
                show_on_lock_switch,
                show_on_lock_state, this,
                calendarKey, Keys.SHOW_ON_LOCK, showOnLock);
        
        addSwitchChangeListener(
                adaptive_color_switch,
                adaptiveColor_switch_state, this,
                calendarKey, Keys.ADAPTIVE_COLOR_ENABLED, adaptiveColorEnabled);
    }
    
    public void setStateIconColor(TextView display, String parameter) {
        int keyIndex = getFirstValidKeyIndex(calendarSubKeys, parameter);
        if (keyIndex == calendarSubKeys.size() - 1) {
            display.setTextColor(Color.GREEN);
        } else if (keyIndex >= 0) {
            display.setTextColor(Color.YELLOW);
        } else {
            display.setTextColor(Color.GRAY);
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
