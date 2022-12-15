package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.settings_entries.CalendarAccountSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CalendarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarSettingsFragment extends BaseSettingsFragment {
    
    private SystemCalendarSettings systemCalendarSettings;
    
    // initial window creation
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        systemCalendarSettings = new SystemCalendarSettings(null, requireContext(), getLifecycle());
        
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
        
        List<SettingsEntryConfig> settingsEntries = new ArrayList<>();
        settingsEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_system_calendars)));
        settingsListViewAdapter = new SettingsListViewAdapter(settingsEntries, getLifecycle());
        
        List<SettingsEntryConfig> additionalSettingsEntries = new ArrayList<>();
        
        new Thread(() -> {
            List<SystemCalendar> calendars = getAllCalendars(requireContext(), true);
            List<List<SystemCalendar>> sortedCalendars = new ArrayList<>();
            List<String> sortedCalendarNames = new ArrayList<>();
            
            for (SystemCalendar calendar : calendars) {
                if (sortedCalendarNames.contains(calendar.account_name)) {
                    sortedCalendars.get(sortedCalendarNames.indexOf(calendar.account_name)).add(calendar);
                } else {
                    List<SystemCalendar> calendarGroup = new ArrayList<>();
                    calendarGroup.add(calendar);
                    sortedCalendars.add(calendarGroup);
                    sortedCalendarNames.add(calendar.account_name);
                }
            }
            
            for (List<SystemCalendar> calendarGroup : sortedCalendars) {
                
                additionalSettingsEntries.add(new CalendarAccountSettingsEntryConfig(systemCalendarSettings, calendarGroup.get(0)));
                
                for (SystemCalendar currentCalendar : calendarGroup) {
                    additionalSettingsEntries.add(new CalendarSettingsEntryConfig(systemCalendarSettings, currentCalendar));
                }
            }
            requireActivity().runOnUiThread(() -> {
                int offset = settingsEntries.size();
                settingsEntries.addAll(additionalSettingsEntries);
                settingsListViewAdapter.notifyItemRangeInserted(offset, offset + additionalSettingsEntries.size());
            });
        }, "SSCFetch thread").start();
    }
    
    // full destroy
    @Override
    public void onDestroy() {
        systemCalendarSettings = null;
        super.onDestroy();
    }
}
