package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarKeyToReadable;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidBooleanValue;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidIntValue;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKeyIndex;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Utilities.setSliderChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.setSwitchChangeListener;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import java.util.List;
import java.util.Set;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.PreferencesStore;
import prototype.xd.scheduler.utilities.TodoListEntryManager;

public class SystemCalendarSettings extends PopupSettingsView {
    
    // if called from regular settings
    private List<String> calendarSubKeys;
    private String calendarKey;
    
    // if called from main screen
    private TodoListEntry todoListEntry;
    
    private TextWatcher currentListener;
    
    public SystemCalendarSettings(@Nullable final TodoListEntryManager todoListEntryManager,
                                  @NonNull final Context context,
                                  @NonNull final Lifecycle lifecycle) {
        super(context, todoListEntryManager, lifecycle);
        bnd.groupSelector.setVisibility(View.GONE);
    }
    
    public void show(final String calendarKey) {
        initialize(calendarKey);
        dialog.show();
    }
    
    public void show(final TodoListEntry entry) {
        this.todoListEntry = entry;
        initialize(entry.event.getKey());
        dialog.show();
    }
    
    private void initialize(final String calendarKey) {
        
        bnd.entrySettingsTitle.setText(calendarKeyToReadable(dialog.getContext(), calendarKey));
        
        this.calendarKey = calendarKey;
        calendarSubKeys = generateSubKeysFromKey(calendarKey);
        
        updatePreviews(
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        
        updateAllIndicators();
        
        bnd.settingsResetButton.setOnClickListener(v ->
                displayConfirmationDialogue(v.getContext(), lifecycle,
                        R.string.reset_settings_prompt,
                        R.string.cancel, R.string.reset, v1 -> {
                            Set<String> preferenceKeys = preferences.getAll().keySet();
                            SharedPreferences.Editor editor = preferences.edit();
                            for (String preferenceKey : preferenceKeys) {
                                if (preferenceKey.startsWith(calendarKey)) {
                                    editor.remove(preferenceKey);
                                }
                            }
                            editor.apply();
                            if (todoListEntry != null) {
                                todoListEntry.event.invalidateAllParametersOfConnectedEntries();
                            }
                            initialize(calendarKey);
                        }));
        
        bnd.fontColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.fontColorState, this,
                Keys.FONT_COLOR,
                parameterKey -> getFirstValidIntValue(calendarSubKeys, parameterKey, Keys.SETTINGS_DEFAULT_FONT_COLOR)));
        
        bnd.backgroundColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.backgroundColorState, this,
                Keys.BG_COLOR,
                parameterKey -> getFirstValidIntValue(calendarSubKeys, parameterKey, Keys.SETTINGS_DEFAULT_BG_COLOR)));
        
        bnd.borderColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.borderColorState, this,
                Keys.BORDER_COLOR,
                parameterKey -> getFirstValidIntValue(calendarSubKeys, parameterKey, Keys.SETTINGS_DEFAULT_BORDER_COLOR)));
        
        setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessBar, bnd.borderThicknessState,
                this, bnd.previewBorder, R.string.settings_border_thickness,
                Keys.BORDER_THICKNESS,
                parameterKey -> getFirstValidIntValue(calendarSubKeys, parameterKey, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        
        setSliderChangeListener(
                bnd.priorityDescription,
                bnd.priorityBar, bnd.priorityState,
                this, null, R.string.settings_priority,
                Keys.PRIORITY,
                parameterKey -> getFirstValidIntValue(calendarSubKeys, parameterKey, Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY));
        
        setSliderChangeListener(
                bnd.adaptiveColorBalanceDescription,
                bnd.adaptiveColorBalanceBar, bnd.adaptiveColorBalanceState,
                this, null, R.string.settings_adaptive_color_balance,
                Keys.ADAPTIVE_COLOR_BALANCE,
                parameterKey -> getFirstValidIntValue(calendarSubKeys, parameterKey, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE));
        
        setSliderChangeListener(
                bnd.showDaysUpcomingDescription,
                bnd.showDaysUpcomingBar, bnd.showDaysUpcomingState,
                this, null, R.string.settings_show_days_upcoming,
                Keys.UPCOMING_ITEMS_OFFSET,
                parameterKey -> getFirstValidIntValue(calendarSubKeys, parameterKey, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET));
        
        setSliderChangeListener(
                bnd.showDaysExpiredDescription,
                bnd.showDaysExpiredBar, bnd.showDaysExpiredState,
                this, null, R.string.settings_show_days_expired,
                Keys.EXPIRED_ITEMS_OFFSET,
                parameterKey -> getFirstValidIntValue(calendarSubKeys, parameterKey, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET),
                (slider, value, fromUser) ->
                        bnd.hideExpiredItemsByTimeSwitch.setTextColor(value == 0 ?
                                defaultTextColor :
                                slider.getContext().getColor(R.color.entry_settings_parameter_group_and_personal)), null);
        
        setSwitchChangeListener(
                bnd.hideExpiredItemsByTimeSwitch,
                bnd.hideExpiredItemsByTimeState, this,
                Keys.HIDE_EXPIRED_ENTRIES_BY_TIME,
                parameterKey -> getFirstValidBooleanValue(calendarSubKeys, parameterKey, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME));
        
        setSwitchChangeListener(
                bnd.showOnLockSwitch,
                bnd.showOnLockState, this,
                Keys.SHOW_ON_LOCK,
                parameterKey -> getFirstValidBooleanValue(calendarSubKeys, parameterKey, Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK));
        
        setSwitchChangeListener(
                bnd.hideByContentSwitch,
                bnd.hideByContentSwitchState, this,
                Keys.HIDE_ENTRIES_BY_CONTENT,
                parameterKey -> getFirstValidBooleanValue(calendarSubKeys, parameterKey, Keys.SETTINGS_DEFAULT_HIDE_ENTRIES_BY_CONTENT));
        
        bnd.hideByContentField.setText(preferences.getString(getFirstValidKey(calendarSubKeys, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT), ""));
        if (currentListener != null) {
            bnd.hideByContentField.removeTextChangedListener(currentListener);
        }
        currentListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //ignore
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // sometimes this listener fires just on text field getting focus with count = 0
                if (count != 0) {
                    notifyParameterChanged(bnd.hideByContentFieldState, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT, s.toString());
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                //ignore
            }
        };
        bnd.hideByContentField.addTextChangedListener(currentListener);
    }
    
    @Override
    public <T> void notifyParameterChanged(TextView displayTo, String parameterKey, T value) {
        PreferencesStore.putAny(calendarKey + "_" + parameterKey, value);
        setStateIconColor(displayTo, parameterKey);
        // invalidate parameters on entries in the same calendar category / color
        if (todoListEntry != null) {
            todoListEntry.event.invalidateParameterOfConnectedEntries(parameterKey);
        }
    }
    
    @Override
    public void setStateIconColor(TextView display, String parameterKey) {
        int keyIndex = getFirstValidKeyIndex(calendarSubKeys, parameterKey);
        if (keyIndex == calendarSubKeys.size() - 1) {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_personal));
        } else if (keyIndex >= 0) {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_group));
        } else {
            display.setTextColor(display.getContext().getColor(R.color.entry_settings_parameter_default));
        }
    }
}
