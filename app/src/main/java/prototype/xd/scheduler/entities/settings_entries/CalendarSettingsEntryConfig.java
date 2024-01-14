package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.CALENDAR;
import static prototype.xd.scheduler.utilities.Utilities.getPluralString;

import android.content.res.ColorStateList;
import android.view.View;

import androidx.annotation.NonNull;

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
    private final SystemCalendar calendar;
    @NonNull
    private final String calendarName;
    @NonNull
    private final String calendarVisibilityKey;
    private final int calendarEventsCount;
    
    public CalendarSettingsEntryConfig(@NonNull final SystemCalendar calendar, boolean showSettings) {
        super(showSettings);
        this.calendar = calendar;
        calendarName = calendar.data.displayName;
        calendarVisibilityKey = calendar.visibilityKey;
        calendarEventsCount = calendar.systemCalendarEventMap.size();
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
            
            binding.checkBox.setButtonTintList(ColorStateList.valueOf(config.calendar.data.color));
            binding.checkBox.setCheckedSilent(Static.getBoolean(config.calendarVisibilityKey, Static.CALENDAR_SETTINGS_DEFAULT_VISIBLE));
            binding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    Static.edit().putBoolean(config.calendarVisibilityKey, isChecked).apply());
            
            binding.settingsButton.setOnClickListener(view -> CalendarSettingsDialogFragment.show(config.calendar, wrapper));
            config.updateSettingsButtonVisibility(binding.settingsButton);
            
            if (config.calendarEventsCount > 0) {
                binding.colorSelectButton.setVisibility(View.VISIBLE);
                binding.colorSelectButton.setOnClickListener(v -> CalendarColorSettingsDialogFragment.show(config.calendar, wrapper));
                binding.viewEventsButton.setVisibility(View.VISIBLE);
                binding.viewEventsButton.setOnClickListener(v -> CalendarEventListDialogFragment.show(config.calendar, wrapper));
            } else {
                binding.colorSelectButton.setVisibility(View.GONE);
                binding.viewEventsButton.setVisibility(View.GONE);
            }
        }
    }
}


