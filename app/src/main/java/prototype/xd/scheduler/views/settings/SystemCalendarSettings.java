package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences_service;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarKeyToReadable;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKeyIndex;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;
import static prototype.xd.scheduler.utilities.Utilities.addSliderChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Map;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.TodoListEntryStorage;

public class SystemCalendarSettings extends PopupSettingsView {
    
    private ArrayList<String> calendarSubKeys;
    private TodoListEntry entry;
    private final TodoListEntryStorage todoListEntryStorage;
    
    private TextWatcher currentListener;
    
    public SystemCalendarSettings(@Nullable final TodoListEntryStorage todoListEntryStorage, final View settingsView) {
        super(settingsView);
        
        settingsView.findViewById(R.id.group_selector).setVisibility(View.GONE);
        
        this.todoListEntryStorage = todoListEntryStorage;
        
        dialog = new AlertDialog.Builder(settingsView.getContext(), R.style.FullScreenDialog).setOnDismissListener(dialog -> {
            if (todoListEntryStorage != null) {
                todoListEntryStorage.updateTodoListAdapter(false);
            }
        }).setView(settingsView).create();
    }
    
    public void show(final String calendar_key) {
        initialise(calendar_key);
        dialog.show();
    }
    
    public void show(final TodoListEntry entry) {
        this.entry = entry;
        initialise(makeKey(entry.event));
        dialog.show();
    }
    
    private void initialise(final String calendarKey) {
        
        titleText.setText(calendarKeyToReadable(dialog.getContext(), calendarKey));
        
        calendarSubKeys = generateSubKeysFromKey(calendarKey);
        
        updatePreviews(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        
        updateAllIndicators();
        
        settings_reset_button.setOnClickListener(v ->
                displayConfirmationDialogue(v.getContext(),
                        R.string.reset_settings_prompt,
                        R.string.cancel, R.string.reset, v1 -> {
                            Map<String, ?> allEntries = preferences.getAll();
                            SharedPreferences.Editor editor = preferences.edit();
                            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                                if (entry.getKey().startsWith(calendarKey)) {
                                    editor.remove(entry.getKey());
                                }
                            }
                            editor.apply();
                            initialise(calendarKey);
                        }));
        
        fontColor_select.setOnClickListener(view -> invokeColorDialogue(
                fontColor_view_state, this,
                calendarKey, calendarSubKeys,
                Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR));
        
        bgColor_select.setOnClickListener(view -> invokeColorDialogue(
                bgColor_view_state, this,
                calendarKey, calendarSubKeys,
                Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR));
        
        borderColor_select.setOnClickListener(view -> invokeColorDialogue(
                padColor_view_state, this,
                calendarKey, calendarSubKeys,
                Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR));
        
        addSliderChangeListener(
                border_thickness_description,
                border_thickness_bar, border_size_state,
                this, true, R.string.settings_border_thickness,
                calendarKey, calendarSubKeys,
                Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS);
        
        addSliderChangeListener(
                priority_description,
                priority_bar, priority_state,
                this, false, R.string.settings_priority,
                calendarKey, calendarSubKeys,
                Keys.PRIORITY, Keys.ENTITY_SETTINGS_DEFAULT_PRIORITY);
        
        addSliderChangeListener(
                adaptive_color_balance_description,
                adaptive_color_balance_bar, adaptiveColor_bar_state,
                this, false, R.string.settings_adaptive_color_balance,
                calendarKey, calendarSubKeys,
                Keys.ADAPTIVE_COLOR_BALANCE, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
        
        addSliderChangeListener(
                show_days_beforehand_description,
                show_days_beforehand_bar, showDaysUpcoming_bar_state,
                this, false, R.string.settings_show_days_upcoming,
                calendarKey, calendarSubKeys,
                Keys.UPCOMING_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET);
        
        addSliderChangeListener(
                show_days_after_description,
                show_days_after_bar, showDaysExpired_bar_state,
                this, false, R.string.settings_show_days_expired,
                calendarKey, calendarSubKeys,
                Keys.EXPIRED_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET,
                (slider, value, fromUser) ->
                        hide_expired_items_by_time_switch.setTextColor(slider.getValue() == 0 ?
                                hide_expired_items_by_time_switch_def_colors :
                                ColorStateList.valueOf(slider.getContext().getColor(R.color.entry_settings_parameter_group_and_personal))), null);
        
        addSwitchChangeListener(
                hide_expired_items_by_time_switch,
                hide_expired_items_by_time_state, this,
                calendarKey, calendarSubKeys,
                Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME);
        
        addSwitchChangeListener(
                show_on_lock_switch,
                show_on_lock_state, this,
                calendarKey, calendarSubKeys,
                Keys.SHOW_ON_LOCK, Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
        
        addSwitchChangeListener(
                adaptive_color_switch,
                adaptiveColor_switch_state, this,
                calendarKey, calendarSubKeys,
                Keys.ADAPTIVE_COLOR_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_ENABLED);
        
        addSwitchChangeListener(
                hide_by_content_switch,
                hide_by_content_switch_state, this,
                calendarKey, calendarSubKeys,
                Keys.HIDE_ENTRIES_BY_CONTENT, Keys.SETTINGS_DEFAULT_HIDE_ENTRIES_BY_CONTENT);
        
        hide_by_content_field.setText(preferences.getString(getFirstValidKey(calendarSubKeys, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT), ""));
        if (currentListener != null) {
            hide_by_content_field.removeTextChangedListener(currentListener);
        }
        currentListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                System.out.println(calendarKey);
                preferences.edit().putString(calendarKey + "_" + Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT, s.toString()).apply();
                setStateIconColor(hide_by_content_field_state, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT);
                preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
            }
            
            @Override
            public void afterTextChanged(Editable s) {
            
            }
        };
        hide_by_content_field.addTextChangedListener(currentListener);
    }
    
    @Override
    public void setStateIconColor(TextView display, String parameter) {
        int keyIndex = getFirstValidKeyIndex(calendarSubKeys, parameter);
        if (keyIndex == calendarSubKeys.size() - 1) {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_personal));
        } else if (keyIndex >= 0) {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_group));
        } else {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_default));
        }
        if (todoListEntryStorage != null) {
            for (TodoListEntry current_entry : todoListEntryStorage.getTodoListEntries()) {
                if (current_entry.fromSystemCalendar) {
                    if (current_entry.event.subKeys.equals(entry.event.subKeys)) {
                        current_entry.reloadParams();
                    }
                }
            }
        }
    }
}
