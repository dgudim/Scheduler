package prototype.xd.scheduler.fragments;

import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayMessageDialog;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.getAllCalendars;

import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.settings_entries.CalendarAccountSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CalendarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.GenericCalendarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarSettingsFragment extends BaseListSettingsFragment<ConcatAdapter> {
    
    // initial window creation
    @MainThread
    @Override
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SystemCalendarSettings systemCalendarSettings = new SystemCalendarSettings(wrapper, null);
        
        //                                                       two entries - title and the switch
        List<SettingsEntryConfig> staticEntries = new ArrayList<>(2);
        staticEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_system_calendars)));
        SettingsListViewAdapter staticEntriesListViewAdapter = new SettingsListViewAdapter(wrapper, staticEntries);
        
        listViewAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false)
                .setStableIdMode(NO_STABLE_IDS).build(),
                staticEntriesListViewAdapter);
        
        ArrayList<GenericCalendarSettingsEntryConfig> calendarConfigEntries = new ArrayList<>();
        
        new Thread(() -> {
            List<SystemCalendar> calendars = getAllCalendars(requireContext(), true);
            Map<String, List<SystemCalendar>> calendarGroups = new TreeMap<>();
            
            for (SystemCalendar calendar : calendars) {
                calendarGroups.computeIfAbsent(calendar.accountName,
                                e -> new ArrayList<>())
                        .add(calendar);
            }
            
            // set the capacity to the number of calendars + the number of groups
            calendarConfigEntries.ensureCapacity(calendars.size() + calendarGroups.size());
            
            boolean showSettings = Static.ALLOW_GLOBAL_CALENDAR_ACCOUNT_SETTINGS.get();
            
            for (List<SystemCalendar> calendarGroup : calendarGroups.values()) {
                
                ArrayList<GenericCalendarSettingsEntryConfig> calendarEntryList = new ArrayList<>(calendarGroup.size() + 1);
                SettingsListViewAdapter calendarEntryListAdapter = new SettingsListViewAdapter(wrapper, calendarEntryList, true);
                
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
                Static.ALLOW_GLOBAL_CALENDAR_ACCOUNT_SETTINGS,
                getString(R.string.settings_allow_global_calendar_account_settings), (buttonView, isChecked) -> {
            if (isChecked) {
                displayMessageDialog(wrapper, builder -> {
                    builder.setTitle(R.string.attention);
                    builder.setMessage(R.string.whole_calendar_settings_on_warning);
                    builder.setIcon(R.drawable.ic_warning_24_onerrorcontainer);
                    builder.setPositiveButton(R.string.i_understand, null);
                });
            } else {
                displayMessageDialog(wrapper, builder -> {
                    builder.setTitle(R.string.attention);
                    builder.setMessage(R.string.whole_calendar_settings_off_warning);
                    builder.setIcon(R.drawable.ic_warning_24_onerrorcontainer);
                    builder.setPositiveButton(R.string.i_understand, null);
                });
            }
            for (GenericCalendarSettingsEntryConfig calendarEntryConfig : calendarConfigEntries) {
                calendarEntryConfig.setShowSettings(isChecked);
            }
            // almost all the list is updated (except first entry)
            listViewAdapter.notifyItemRangeChanged(staticEntries.size(), calendarConfigEntries.size());
        }, false));
        staticEntriesListViewAdapter.notifyItemInserted(staticEntries.size());
    }
}
