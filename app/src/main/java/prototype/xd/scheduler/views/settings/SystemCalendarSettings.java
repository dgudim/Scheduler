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
    public SystemCalendarSettings(final SettingsFragment fragment, View settingsView, final String calendar_key) {
        super(settingsView);
        
        settingsView.findViewById(R.id.group_selector).setVisibility(View.GONE);
        
        calendarSubKeys = generateSubKeysFromKey(calendar_key);
        
        initialise(calendar_key, fragment.context, fragment);
        
        new AlertDialog.Builder(fragment.context).setView(settingsView).show();
    }
    
    private void initialise(String calendarKey, final Context context, final SettingsFragment fragment) {
    
        fontColor_view.setBackgroundColor(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR));
        bgColor_view.setBackgroundColor(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR));
        padColor_view.setBackgroundColor(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEVEL_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR));
        
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
                calendarKey, Keys.FONT_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_TODAY_FONT_COLOR), true));
        
        bgColor_view.setOnClickListener(view -> invokeColorDialogue(context,
                view, bgColor_view_state,
                this,
                calendarKey, Keys.BG_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BG_COLOR), true));
        
        padColor_view.setOnClickListener(view -> invokeColorDialogue(context,
                view, padColor_view_state,
                this,
                calendarKey, Keys.BEVEL_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEVEL_COLOR), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_COLOR), true));
        
        addSeekBarChangeListener(
                bevel_thickness_description,
                bevel_thickness_bar,
                padSize_state, this, fragment, R.string.settings_bevel_thickness,
                calendarKey, Keys.BEVEL_THICKNESS,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BEVEL_THICKNESS), Keys.SETTINGS_DEFAULT_TODAY_BEVEL_THICKNESS));
        
        addSeekBarChangeListener(
                priority_description,
                priority_bar,
                priority_state, this, fragment, R.string.settings_priority,
                calendarKey, Keys.PRIORITY,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.PRIORITY), Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY));
        
        addSeekBarChangeListener(
                adaptive_color_balance_description,
                adaptive_color_balance_bar,
                adaptiveColor_bar_state, this, fragment, R.string.settings_adaptive_color_balance,
                calendarKey, Keys.ADAPTIVE_COLOR_BALANCE,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE));
        
        addSeekBarChangeListener(
                show_days_beforehand_description,
                show_days_beforehand_bar,
                showDaysUpcoming_bar_state, this, fragment, R.string.settings_show_days_upcoming,
                calendarKey, Keys.UPCOMING_ITEMS_OFFSET,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.UPCOMING_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET));
        
        addSeekBarChangeListener(
                show_days_after_description,
                show_days_after_bar,
                showDaysExpired_bar_state, this, fragment, R.string.settings_show_days_expired,
                calendarKey, Keys.EXPIRED_ITEMS_OFFSET,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.EXPIRED_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET));
        
        addSwitchChangeListener(
                show_on_lock_switch,
                show_on_lock_state, this,
                calendarKey, Keys.SHOW_ON_LOCK,
                preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.SHOW_ON_LOCK), Keys.SETTINGS_DEFAULT_SHOW_ON_LOCK));
        
        addSwitchChangeListener(
                adaptive_color_switch,
                adaptiveColor_switch_state, this,
                calendarKey, Keys.ADAPTIVE_COLOR_ENABLED,
                preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_ENABLED), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED));
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
        setStateIconColor(showDaysUpcoming_bar_state, Keys.UPCOMING_ITEMS_OFFSET);
        setStateIconColor(showDaysExpired_bar_state, Keys.EXPIRED_ITEMS_OFFSET);
    }
}
