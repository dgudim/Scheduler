package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.CALENDAR;
import static prototype.xd.scheduler.utilities.Utilities.getPluralString;

import android.content.res.ColorStateList;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CalendarSettingsEntryBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.fragments.dialogs.CalendarColorSettingsDialogFragment;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarSettingsEntryConfig extends GenericCalendarSettingsEntryConfig {
    
    @NonNull
    private final SystemCalendarSettings systemCalendarSettings;
    @NonNull
    private final SystemCalendar calendar;
    @NonNull
    private final String calendarName;
    @NonNull
    private final String calendarPrefKey;
    @NonNull
    private final List<String> calendarSubKeys;
    @NonNull
    private final String calendarVisibilityKey;
    private final int calendarEventsCount;
    private final int calendarColor;
    
    @Nullable
    private final CalendarColorSettingsDialogFragment colorSettingsDialogFragment;
    
    public CalendarSettingsEntryConfig(@NonNull final SystemCalendarSettings settings,
                                       @NonNull final SystemCalendar calendar,
                                       boolean showSettings) {
        super(showSettings);
        systemCalendarSettings = settings;
        this.calendar = calendar;
        calendarName = calendar.data.displayName;
        calendarPrefKey = calendar.prefKey;
        calendarVisibilityKey = calendar.visibilityKey;
        calendarSubKeys = calendar.subKeys;
        calendarColor = calendar.data.color;
        calendarEventsCount = calendar.systemCalendarEventMap.size();
        
        if (!calendar.eventColorCountMap.isEmpty() && calendarEventsCount > 0) {
            colorSettingsDialogFragment = new CalendarColorSettingsDialogFragment();
        } else {
            colorSettingsDialogFragment = null;
        }
    }
    
    private void showColorSettingsDialog(@NonNull ContextWrapper wrapper) {
        Objects.requireNonNull(colorSettingsDialogFragment).show(systemCalendarSettings, calendar, wrapper);
    }
    
    @Override
    public int getRecyclerViewType() {
        return CALENDAR.ordinal();
    }
    
    static class ViewHolder extends SettingsEntryConfig.SettingsViewHolder<CalendarSettingsEntryBinding, CalendarSettingsEntryConfig> {
        
        ViewHolder(@NonNull ContextWrapper wrapper, @NonNull CalendarSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(@NonNull CalendarSettingsEntryConfig config) {
            viewBinding.calendarName.setText(config.calendarName);
            viewBinding.eventCount.setText(getPluralString(wrapper.context, R.plurals.calendar_event_count, config.calendarEventsCount));
            
            viewBinding.checkBox.setButtonTintList(ColorStateList.valueOf(config.calendarColor));
            viewBinding.checkBox.setCheckedSilent(Static.getBoolean(config.calendarVisibilityKey, Static.CALENDAR_SETTINGS_DEFAULT_VISIBLE));
            viewBinding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    Static.edit().putBoolean(config.calendarVisibilityKey, isChecked).apply());
            
            viewBinding.settingsButton.setOnClickListener(view ->
                    config.systemCalendarSettings.show(
                            config.calendarPrefKey, config.calendarSubKeys,
                            config.calendarColor, wrapper));
            config.updateSettingsButtonVisibility(viewBinding.settingsButton);
            
            if (config.colorSettingsDialogFragment != null) {
                viewBinding.colorSelectButton.setVisibility(View.VISIBLE);
                viewBinding.colorSelectButton.setOnClickListener(v -> config.showColorSettingsDialog(wrapper));
            } else {
                viewBinding.colorSelectButton.setVisibility(View.GONE);
            }
        }
    }
}


