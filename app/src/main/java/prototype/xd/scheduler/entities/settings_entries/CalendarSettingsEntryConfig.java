package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.CALENDAR;
import static prototype.xd.scheduler.utilities.Utilities.getPluralString;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.GridView;

import androidx.annotation.NonNull;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.CalendarColorsGridViewAdapter;
import prototype.xd.scheduler.databinding.CalendarSettingsEntryBinding;
import prototype.xd.scheduler.databinding.GridSelectionViewBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarSettingsEntryConfig extends GenericCalendarSettingsEntryConfig {
    
    private final SystemCalendarSettings systemCalendarSettings;
    private final String calendarName;
    private final String calendarKey;
    private final int calendarEventsCount;
    private final int calendarColor;
    
    private CalendarColorsGridViewAdapter gridViewAdapter;
    
    public CalendarSettingsEntryConfig(final SystemCalendarSettings systemCalendarSettings,
                                       final SystemCalendar calendar,
                                       boolean showSettings) {
        super(showSettings);
        this.systemCalendarSettings = systemCalendarSettings;
        this.calendarName = calendar.displayName;
        calendarKey = calendar.getKey();
        this.calendarColor = calendar.color;
        calendarEventsCount = calendar.systemCalendarEvents.size();
        
        if (calendar.eventColorCountMap.size() > 0 && calendarEventsCount > 0) {
            gridViewAdapter = new CalendarColorsGridViewAdapter(systemCalendarSettings, calendar);
        }
    }
    
    @Override
    public int getType() {
        return CALENDAR.ordinal();
    }
    
    static class CalendarViewHolder extends SettingsEntryConfig.SettingsViewHolder<CalendarSettingsEntryBinding, CalendarSettingsEntryConfig> {
        
        CalendarViewHolder(@NonNull ContextWrapper wrapper, @NonNull CalendarSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(CalendarSettingsEntryConfig config) {
            viewBinding.calendarName.setText(config.calendarName);
            viewBinding.eventCount.setText(getPluralString(wrapper.context, R.plurals.calendar_event_count, config.calendarEventsCount));
            
            viewBinding.checkBox.setButtonTintList(ColorStateList.valueOf(config.calendarColor));
            viewBinding.checkBox.setCheckedSilent(Keys.getBoolean(config.calendarKey + "_" + Keys.VISIBLE, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE));
            viewBinding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    Keys.edit().putBoolean(config.calendarKey + "_" + Keys.VISIBLE, isChecked).apply());
            
            viewBinding.settingsButton.setOnClickListener(view -> config.systemCalendarSettings.show(config.calendarKey, config.calendarColor));
            config.updateSettingsButtonVisibility(viewBinding.settingsButton);
            
            if (config.gridViewAdapter != null) {
                viewBinding.colorSelectButton.setVisibility(View.VISIBLE);
                viewBinding.colorSelectButton.setOnClickListener(v -> {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
                    
                    GridSelectionViewBinding gridSelection = GridSelectionViewBinding.inflate(wrapper.getLayoutInflater());
                    GridView gridView = gridSelection.gridView;
                    gridView.setNumColumns(2);
                    gridView.setHorizontalSpacing(5);
                    gridView.setVerticalSpacing(5);
                    gridView.setAdapter(config.gridViewAdapter);
                    
                    alert.setView(gridSelection.getRoot());
                    alert.show();
                });
            } else {
                viewBinding.colorSelectButton.setVisibility(View.GONE);
            }
        }
    }
}


