package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarKeyToReadable;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getFirstValidKeyIndex;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;
import static prototype.xd.scheduler.utilities.Utilities.setSliderChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.setSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.TodoListEntryManager;
import prototype.xd.scheduler.utilities.Utilities;

public class SystemCalendarSettings extends PopupSettingsView {
    
    private List<String> calendarSubKeys;
    private TodoListEntry entry;
    private final TodoListEntryManager todoListEntryManager;
    
    private TextWatcher currentListener;
    
    public SystemCalendarSettings(@Nullable final TodoListEntryManager todoListEntryManager, @NonNull final Context context) {
        super(context);
        
        bnd.groupSelector.setVisibility(View.GONE);
        
        this.todoListEntryManager = todoListEntryManager;
        
        dialog = new AlertDialog.Builder(context, R.style.FullScreenDialog)
                .setOnDismissListener(dialog -> {
                    if (todoListEntryManager != null) {
                        // TODO: 20.11.2022 handle entry updates
                        todoListEntryManager.setBitmapUpdateFlag(false);
                    }
                }).setView(bnd.getRoot()).create();
    }
    
    public void show(final String calendarKey) {
        initialise(calendarKey);
        dialog.show();
    }
    
    public void show(final TodoListEntry entry) {
        this.entry = entry;
        initialise(makeKey(entry.event));
        dialog.show();
    }
    
    private void initialise(final String calendarKey) {
        
        bnd.entrySettingsTitle.setText(calendarKeyToReadable(dialog.getContext(), calendarKey));
        
        calendarSubKeys = generateSubKeysFromKey(calendarKey);
        
        updatePreviews(preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.FONT_COLOR), Keys.SETTINGS_DEFAULT_FONT_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BG_COLOR), Keys.SETTINGS_DEFAULT_BG_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_COLOR), Keys.SETTINGS_DEFAULT_BORDER_COLOR),
                preferences.getInt(getFirstValidKey(calendarSubKeys, Keys.BORDER_THICKNESS), Keys.SETTINGS_DEFAULT_BORDER_THICKNESS));
        
        updateAllIndicators();
        
        bnd.settingsResetButton.setOnClickListener(v ->
                displayConfirmationDialogue(v.getContext(),
                        R.string.reset_settings_prompt,
                        R.string.cancel, R.string.reset, v1 -> {
                            Map<String, ?> allPreferences = preferences.getAll();
                            SharedPreferences.Editor editor = preferences.edit();
                            for (Map.Entry<String, ?> preferenceEntry : allPreferences.entrySet()) {
                                if (preferenceEntry.getKey().startsWith(calendarKey)) {
                                    editor.remove(preferenceEntry.getKey());
                                }
                            }
                            editor.apply();
                            initialise(calendarKey);
                        }));
        
        bnd.fontColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.fontColorState, this,
                calendarKey, calendarSubKeys,
                Keys.FONT_COLOR, Keys.SETTINGS_DEFAULT_FONT_COLOR));
        
        bnd.backgroundColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.backgroundColorState, this,
                calendarKey, calendarSubKeys,
                Keys.BG_COLOR, Keys.SETTINGS_DEFAULT_BG_COLOR));
        
        bnd.borderColorSelector.setOnClickListener(view -> invokeColorDialogue(
                bnd.borderColorState, this,
                calendarKey, calendarSubKeys,
                Keys.BORDER_COLOR, Keys.SETTINGS_DEFAULT_BORDER_COLOR));
        
        setSliderChangeListener(
                bnd.borderThicknessDescription,
                bnd.borderThicknessBar, bnd.borderThicknessState,
                this, bnd.previewBorder, R.string.settings_border_thickness,
                calendarKey, calendarSubKeys,
                Keys.BORDER_THICKNESS, Keys.SETTINGS_DEFAULT_BORDER_THICKNESS);
        
        setSliderChangeListener(
                bnd.priorityDescription,
                bnd.priorityBar, bnd.priorityState,
                this, null, R.string.settings_priority,
                calendarKey, calendarSubKeys,
                Keys.PRIORITY, Keys.ENTRY_SETTINGS_DEFAULT_PRIORITY);
        
        setSliderChangeListener(
                bnd.adaptiveColorBalanceDescription,
                bnd.adaptiveColorBalanceBar, bnd.adaptiveColorBalanceState,
                this, null, R.string.settings_adaptive_color_balance,
                calendarKey, calendarSubKeys,
                Keys.ADAPTIVE_COLOR_BALANCE, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE);
        
        setSliderChangeListener(
                bnd.showDaysUpcomingDescription,
                bnd.showDaysUpcomingBar, bnd.showDaysUpcomingState,
                this, null, R.string.settings_show_days_upcoming,
                calendarKey, calendarSubKeys,
                Keys.UPCOMING_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET);
        
        Utilities.setSliderChangeListener(
                bnd.showDaysExpiredDescription,
                bnd.showDaysExpiredBar, bnd.showDaysExpiredState,
                this, null, R.string.settings_show_days_expired,
                calendarKey, calendarSubKeys,
                Keys.EXPIRED_ITEMS_OFFSET, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET,
                (slider, value, fromUser) ->
                        bnd.hideExpiredItemsByTimeSwitch.setTextColor(value == 0 ?
                                defaultTextColor :
                                slider.getContext().getColor(R.color.entry_settings_parameter_group_and_personal)), null);
        
        setSwitchChangeListener(
                bnd.hideExpiredItemsByTimeSwitch,
                bnd.hideExpiredItemsByTimeState, this,
                calendarKey, calendarSubKeys,
                Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME);
        
        setSwitchChangeListener(
                bnd.showOnLockSwitch,
                bnd.showOnLockState, this,
                calendarKey, calendarSubKeys,
                Keys.SHOW_ON_LOCK, Keys.CALENDAR_SETTINGS_DEFAULT_SHOW_ON_LOCK);
        
        setSwitchChangeListener(
                bnd.hideByContentSwitch,
                bnd.hideByContentSwitchState, this,
                calendarKey, calendarSubKeys,
                Keys.HIDE_ENTRIES_BY_CONTENT, Keys.SETTINGS_DEFAULT_HIDE_ENTRIES_BY_CONTENT);
        
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
                preferences.edit().putString(calendarKey + "_" + Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT, s.toString()).apply();
                setStateIconColor(bnd.hideByContentFieldState, Keys.HIDE_ENTRIES_BY_CONTENT_CONTENT);
                servicePreferences.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                //ignore
            }
        };
        bnd.hideByContentField.addTextChangedListener(currentListener);
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
        // invalidate parameters on entries in the same calendar category / color
        if (todoListEntryManager != null) {
            for (TodoListEntry currentEntry : todoListEntryManager.getTodoListEntries()) {
                if (currentEntry.isFromSystemCalendar() && currentEntry.event.subKeys.equals(entry.event.subKeys)) {
                    currentEntry.invalidateParameter(parameter, TodoListEntry.EntryType.TODAY);
                }
            }
        }
    }
}
