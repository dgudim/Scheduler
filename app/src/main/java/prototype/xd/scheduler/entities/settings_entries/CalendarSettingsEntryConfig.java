package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.CALENDAR;
import static prototype.xd.scheduler.utilities.Utilities.getPluralString;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.GridView;

import androidx.annotation.NonNull;

import java.util.List;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.CalendarColorsGridViewAdapter;
import prototype.xd.scheduler.databinding.CalendarSettingsEntryBinding;
import prototype.xd.scheduler.databinding.GridSelectionViewBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.utilities.DialogUtilities;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarSettingsEntryConfig extends GenericCalendarSettingsEntryConfig {
    
    @NonNull
    private final SystemCalendarSettings systemCalendarSettings;
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
    
    private CalendarColorsGridViewAdapter gridViewAdapter;
    
    public CalendarSettingsEntryConfig(@NonNull final SystemCalendarSettings settings,
                                       @NonNull final SystemCalendar calendar,
                                       boolean showSettings,
                                       @NonNull final ContextWrapper wrapper) {
        super(showSettings);
        systemCalendarSettings = settings;
        calendarName = calendar.data.displayName;
        calendarPrefKey = calendar.prefKey;
        calendarVisibilityKey = calendar.visibilityKey;
        calendarSubKeys = calendar.subKeys;
        calendarColor = calendar.data.color;
        calendarEventsCount = calendar.systemCalendarEventMap.size();
        
        if (!calendar.eventColorCountMap.isEmpty() && calendarEventsCount > 0) {
            gridViewAdapter = new CalendarColorsGridViewAdapter(settings, calendar, wrapper);
        }
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
            
            if (config.gridViewAdapter != null) {
                viewBinding.colorSelectButton.setVisibility(View.VISIBLE);
                viewBinding.colorSelectButton.setOnClickListener(v -> DialogUtilities.displayMessageDialog(
                        wrapper, builder -> {
                            GridSelectionViewBinding gridSelection = GridSelectionViewBinding.inflate(wrapper.getLayoutInflater());
                            GridView gridView = gridSelection.gridView;
                            gridView.setNumColumns(2);
                            gridView.setHorizontalSpacing(5);
                            gridView.setVerticalSpacing(5);
                            gridView.setAdapter(config.gridViewAdapter);
                            
                            builder.setIcon(R.drawable.ic_palette_45);
                            builder.setTitle(R.string.title_edit_events_with_color);
                            builder.setMessage(R.string.title_edit_events_with_color_description);
                            builder.setNegativeButton(R.string.close, null);
                            builder.setView(gridSelection.getRoot());
                        }
                ));
            } else {
                viewBinding.colorSelectButton.setVisibility(View.GONE);
            }
        }
    }
}


