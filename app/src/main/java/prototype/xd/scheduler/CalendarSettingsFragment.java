package prototype.xd.scheduler;

import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayMessageDialog;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.settings_entries.CalendarAccountSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CalendarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.GenericCalendarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarSettingsFragment extends BaseListSettingsFragment<ConcatAdapter> {
    
    private SystemCalendarSettings systemCalendarSettings;
    
    // initial window creation
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        systemCalendarSettings = new SystemCalendarSettings(null, requireContext(), getLifecycle());
        
        List<SettingsEntryConfig> staticEntries = new ArrayList<>();
        staticEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_system_calendars)));
        SettingsListViewAdapter staticEntriesListViewAdapter = new SettingsListViewAdapter(staticEntries, getLifecycle());
        
        listViewAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false)
                .setStableIdMode(NO_STABLE_IDS).build(),
                staticEntriesListViewAdapter);
        
        List<GenericCalendarSettingsEntryConfig> calendarConfigEntries = new ArrayList<>();
        
        new Thread(() -> {
            List<SystemCalendar> calendars = getAllCalendars(requireContext(), true);
            Map<String, List<SystemCalendar>> sortedCalendars = new TreeMap<>();
            
            for (SystemCalendar calendar : calendars) {
                sortedCalendars.computeIfAbsent(calendar.account_name,
                                s -> new ArrayList<>())
                        .add(calendar);
            }
            
            boolean showSettings = Keys.ALLOW_GLOBAL_CALENDAR_ACCOUNT_SETTINGS.get();
            
            for (List<SystemCalendar> calendarGroup : sortedCalendars.values()) {
                
                ArrayList<GenericCalendarSettingsEntryConfig> calendarEntryList = new ArrayList<>();
                SettingsListViewAdapter calendarEntryListAdapter = new SettingsListViewAdapter(calendarEntryList, getLifecycle(), true);
                
                calendarEntryList.add(new CalendarAccountSettingsEntryConfig(
                        systemCalendarSettings,
                        calendarGroup.get(0),
                        calendarEntryListAdapter,
                        showSettings));
                
                for (SystemCalendar currentCalendar : calendarGroup) {
                    calendarEntryList.add(new CalendarSettingsEntryConfig(
                            systemCalendarSettings,
                            currentCalendar,
                            showSettings));
                }
                
                calendarConfigEntries.addAll(calendarEntryList);
                
                requireActivity().runOnUiThread(() -> listViewAdapter.addAdapter(calendarEntryListAdapter));
            }
        }, "SSCFetch thread").start();
        
        staticEntries.add(new SwitchSettingsEntryConfig(
                Keys.ALLOW_GLOBAL_CALENDAR_ACCOUNT_SETTINGS,
                getString(R.string.settings_allow_global_calendar_account_settings), (buttonView, isChecked) -> {
            if (isChecked) {
                displayMessageDialog(requireContext(), getLifecycle(),
                        R.string.attention, R.string.whole_calendar_settings_on_warning,
                        R.drawable.ic_warning_24_onerrorcontainer, R.string.i_understand,
                        R.style.DefaultAlertDialogTheme,
                        null);
            } else {
                displayMessageDialog(requireContext(), getLifecycle(),
                        R.string.attention, R.string.whole_calendar_settings_off_warning,
                        R.drawable.ic_warning_24_onerrorcontainer, R.string.i_understand,
                        R.style.DefaultAlertDialogTheme,
                        null);
            }
            for (GenericCalendarSettingsEntryConfig calendarEntryConfig : calendarConfigEntries) {
                calendarEntryConfig.setShowSettings(isChecked);
            }
            // almost all the list is updated (except first entry)
            listViewAdapter.notifyItemRangeChanged(staticEntries.size(), calendarConfigEntries.size());
        }, false));
        staticEntriesListViewAdapter.notifyItemInserted(staticEntries.size());
    }
    
    // full destroy
    @Override
    public void onDestroy() {
        systemCalendarSettings = null;
        super.onDestroy();
    }
}
