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

import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Map;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.utilities.Keys;

public class SystemCalendarSettings extends PopupSettingsView {
    
    private final ArrayList<String> calendarSubKeys;
    
    public SystemCalendarSettings(final SettingsFragment fragment, View settingsView, final String calendar_key) {
        super(settingsView);
        
        settingsView.findViewById(R.id.group_selector).setVisibility(View.GONE);
        
        calendarSubKeys = generateSubKeysFromKey(calendar_key);
        
        initialise(calendar_key, fragment);
        
        new AlertDialog.Builder(fragment.context).setView(settingsView).show();
    }
    
    private void initialise(String calendarKey, final SettingsFragment fragment) {
    
        final Context context = fragment.context;
        
        fontColor_view.setCardBackgroundColor(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR));
        bgColor_view.setCardBackgroundColor(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR));
        borderColor_view.setCardBackgroundColor(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR));
        
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
                initialise(calendarKey, fragment);
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        fontColor_view.setOnClickListener(view -> invokeColorDialogue(context,
                (CardView) view, fontColor_view_state,
                this,
                calendarKey, Keys.FONT_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR), true));
        
        bgColor_view.setOnClickListener(view -> invokeColorDialogue(context,
                (CardView) view, bgColor_view_state,
                this,
                calendarKey, Keys.BG_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR), true));
        
        borderColor_view.setOnClickListener(view -> invokeColorDialogue(context,
                (CardView) view, padColor_view_state,
                this,
                calendarKey, Keys.BORDER_COLOR,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR), true));
        
        addSeekBarChangeListener(
                border_thickness_description,
                border_thickness_bar,
                border_size_state, this, fragment, R.string.settings_border_thickness,
                calendarKey, Keys.BORDER_THICKNESS,
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        
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
    
    void updateAllIndicators(Context context) {
        setStateIconColor(fontColor_view_state, Keys.FONT_COLOR, context);
        setStateIconColor(bgColor_view_state, Keys.BG_COLOR, context);
        setStateIconColor(padColor_view_state, Keys.BORDER_COLOR, context);
        setStateIconColor(border_size_state, Keys.BORDER_THICKNESS, context);
        setStateIconColor(priority_state, Keys.PRIORITY, context);
        setStateIconColor(show_on_lock_state, Keys.SHOW_ON_LOCK, context);
        setStateIconColor(adaptiveColor_switch_state, Keys.ADAPTIVE_COLOR_ENABLED, context);
        setStateIconColor(adaptiveColor_bar_state, Keys.ADAPTIVE_COLOR_BALANCE, context);
        setStateIconColor(showDaysUpcoming_bar_state, Keys.UPCOMING_ITEMS_OFFSET, context);
        setStateIconColor(showDaysExpired_bar_state, Keys.EXPIRED_ITEMS_OFFSET, context);
    }
}
