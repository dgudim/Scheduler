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
import prototype.xd.scheduler.fragments.dialogs.CalendarEventListDialogFragment;
import prototype.xd.scheduler.fragments.dialogs.CalendarSettingsDialogFragment;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class CalendarSettingsEntryConfig extends GenericCalendarSettingsEntryConfig {
    
    @NonNull
    private final CalendarSettingsDialogFragment calendarSettingsDialogFragment;
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
    private final CalendarEventListDialogFragment eventListDialogFragment;
    
    public CalendarSettingsEntryConfig(@NonNull final CalendarSettingsDialogFragment settings,
                                       @NonNull final CalendarColorSettingsDialogFragment colorSettingsDialogFragment,
                                       @NonNull final CalendarEventListDialogFragment eventListDialogFragment,
                                       @NonNull final SystemCalendar calendar,
                                       boolean showSettings) {
        super(showSettings);
        calendarSettingsDialogFragment = settings;
        this.calendar = calendar;
        calendarName = calendar.data.displayName;
        calendarPrefKey = calendar.prefKey;
        calendarVisibilityKey = calendar.visibilityKey;
        calendarSubKeys = calendar.subKeys;
        calendarColor = calendar.data.color;
        calendarEventsCount = calendar.systemCalendarEventMap.size();
        
        if (!calendar.eventColorCountMap.isEmpty() && calendarEventsCount > 0) {
            this.colorSettingsDialogFragment = colorSettingsDialogFragment;
        } else {
            this.colorSettingsDialogFragment = null;
        }
        
        this.eventListDialogFragment = eventListDialogFragment;
    }
    
    private void showColorSettingsDialog(@NonNull ContextWrapper wrapper) {
        Objects.requireNonNull(colorSettingsDialogFragment).show(calendarSettingsDialogFragment, calendar, wrapper);
    }
    
    private void showEventListDialog(@NonNull ContextWrapper wrapper) {
        eventListDialogFragment.show(calendar, wrapper);
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
            binding.calendarName.setText(config.calendarName);
            binding.eventCount.setText(getPluralString(wrapper.context, R.plurals.calendar_event_count, config.calendarEventsCount));
            
            binding.checkBox.setButtonTintList(ColorStateList.valueOf(config.calendarColor));
            binding.checkBox.setCheckedSilent(Static.getBoolean(config.calendarVisibilityKey, Static.CALENDAR_SETTINGS_DEFAULT_VISIBLE));
            binding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    Static.edit().putBoolean(config.calendarVisibilityKey, isChecked).apply());
            
            binding.settingsButton.setOnClickListener(view ->
                    config.calendarSettingsDialogFragment.show(
                            config.calendarPrefKey, config.calendarSubKeys,
                            config.calendarColor, wrapper));
            config.updateSettingsButtonVisibility(binding.settingsButton);
            
            if (config.colorSettingsDialogFragment != null) {
                binding.colorSelectButton.setVisibility(View.VISIBLE);
                binding.colorSelectButton.setOnClickListener(v -> config.showColorSettingsDialog(wrapper));
            } else {
                binding.colorSelectButton.setVisibility(View.GONE);
            }
            
            if (config.calendarEventsCount > 0) {
                binding.viewEventsButton.setVisibility(View.VISIBLE);
                binding.viewEventsButton.setOnClickListener(v -> config.showEventListDialog(wrapper));
            } else {
                binding.viewEventsButton.setVisibility(View.GONE);
            }
        }
    }
}


