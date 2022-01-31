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
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.utilities.Keys;

public class SystemCalendarSettings extends PopupSettingsView {
    
    private final AlertDialog dialog;
    private ArrayList<String> calendarSubKeys;
    public final SettingsFragment fragment;
    
    public SystemCalendarSettings(final SettingsFragment fragment, final View settingsView) {
        super(settingsView);
        this.fragment = fragment;
        
        settingsView.findViewById(R.id.group_selector).setVisibility(View.GONE);
        
        dialog = new AlertDialog.Builder(fragment.context).setView(settingsView).create();
    }
    
    public void show(final String calendar_key) {
        initialise(calendar_key);
        dialog.show();
    }
    
    private void initialise(final String calendarKey) {
        
        calendarSubKeys = generateSubKeysFromKey(calendarKey);
        final Context context = fragment.context;
        updatePreviews(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        
        updateAllIndicators(context);
        
        settings_reset_button.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.reset_settings_prompt);
            
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                Map<String, ?> allEntries = preferences.getAll();
                SharedPreferences.Editor editor = preferences.edit();
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    if (entry.getKey().startsWith(calendarKey)) {
                        editor.remove(entry.getKey());
                    }
                }
                editor.apply();
                initialise(calendarKey);
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        fontColor_select.setOnClickListener(view -> invokeColorDialogue(context,
                fontColor_view_state, this,
                calendarKey, Keys.FONT_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR), true));
        
        bgColor_select.setOnClickListener(view -> invokeColorDialogue(context,
                bgColor_view_state, this,
                calendarKey, Keys.BG_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR), true));
        
        borderColor_select.setOnClickListener(view -> invokeColorDialogue(context,
                padColor_view_state, this,
                calendarKey, Keys.BORDER_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR), true));
        
        addSeekBarChangeListener(
                border_thickness_description,
                border_thickness_bar, border_size_state,
                this, true, R.string.settings_border_thickness,
                calendarKey, Keys.BORDER_THICKNESS,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        
        addSeekBarChangeListener(
                priority_description,
                priority_bar, priority_state,
                this, false, R.string.settings_priority,
                calendarKey, Keys.PRIORITY,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.PRIORITY), Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY));
        
        addSeekBarChangeListener(
                adaptive_color_balance_description,
                adaptive_color_balance_bar, adaptiveColor_bar_state,
                this, false, R.string.settings_adaptive_color_balance,
                calendarKey, Keys.ADAPTIVE_COLOR_BALANCE,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_BALANCE), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE));
        
        addSeekBarChangeListener(
                show_days_beforehand_description,
                show_days_beforehand_bar, showDaysUpcoming_bar_state,
                this, false, R.string.settings_show_days_upcoming,
                calendarKey, Keys.UPCOMING_ITEMS_OFFSET,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.UPCOMING_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET));
        
        addSeekBarChangeListener(
                show_days_after_description,
                show_days_after_bar, showDaysExpired_bar_state,
                this, false, R.string.settings_show_days_expired,
                calendarKey, Keys.EXPIRED_ITEMS_OFFSET,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.EXPIRED_ITEMS_OFFSET), Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET));
        
        addSwitchChangeListener(context,
                show_on_lock_switch,
                show_on_lock_state, this,
                calendarKey, Keys.SHOW_ON_LOCK,
                preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.SHOW_ON_LOCK), Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK));
        
        addSwitchChangeListener(context,
                adaptive_color_switch,
                adaptiveColor_switch_state, this,
                calendarKey, Keys.ADAPTIVE_COLOR_ENABLED,
                preferences.getBoolean(getFirstValidKey(calendarSubKeys, Keys.ADAPTIVE_COLOR_ENABLED), Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED));
    }
    
    @Override
    public void setStateIconColor(TextView display, String parameter, Context context) {
        int keyIndex = getFirstValidKeyIndex(calendarSubKeys, parameter);
        if (keyIndex == calendarSubKeys.size() - 1) {
            display.setTextColor(context.getColor(R.color.entry_settings_parameter_personal));
        } else if (keyIndex >= 0) {
            display.setTextColor(context.getColor(R.color.entry_settings_parameter_group));
        } else {
            display.setTextColor(context.getColor(R.color.entry_settings_parameter_default));
        }
    }
}
