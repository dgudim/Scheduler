package prototype.xd.scheduler;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.entities.settings_entries.AdaptiveBackgroundSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.AppThemeSelectorEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CompoundCustomizationEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.ResetButtonSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SeekBarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.utilities.Keys;

public class GlobalSettingsFragment extends BaseSettingsFragment {
    
    private AdaptiveBackgroundSettingsEntryConfig adaptiveBackgroundSettingsEntry;
    
    // initial window creation
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
        
        List<SettingsEntryConfig> settingsEntries = new ArrayList<>();
        
        adaptiveBackgroundSettingsEntry = new AdaptiveBackgroundSettingsEntryConfig(this);
        
        settingsEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_appearance)));
        settingsEntries.add(new AppThemeSelectorEntryConfig());
        settingsEntries.add(adaptiveBackgroundSettingsEntry);
        settingsEntries.add(new SeekBarSettingsEntryConfig(0, 10, Keys.SETTINGS_DEFAULT_ADAPTIVE_COLOR_BALANCE, true, true,
                Keys.ADAPTIVE_COLOR_BALANCE, R.string.settings_adaptive_color_balance));
        settingsEntries.add(new CompoundCustomizationEntryConfig());
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.ITEM_FULL_WIDTH_LOCK, Keys.SETTINGS_DEFAULT_ITEM_FULL_WIDTH_LOCK,
                getString(R.string.settings_max_rWidth_lock)));
        
        settingsEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_visibility)));
        settingsEntries.add(new SeekBarSettingsEntryConfig(0, 14, Keys.SETTINGS_DEFAULT_UPCOMING_ITEMS_OFFSET, true, false,
                Keys.UPCOMING_ITEMS_OFFSET, R.string.settings_show_days_upcoming));
        settingsEntries.add(new SeekBarSettingsEntryConfig(0, 14, Keys.SETTINGS_DEFAULT_EXPIRED_ITEMS_OFFSET, true, false,
                Keys.EXPIRED_ITEMS_OFFSET, R.string.settings_show_days_expired));
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, Keys.SETTINGS_DEFAULT_HIDE_EXPIRED_ENTRIES_BY_TIME,
                getString(R.string.settings_hide_expired_entries_by_time)));
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.SHOW_UPCOMING_EXPIRED_IN_LIST, Keys.SETTINGS_DEFAULT_SHOW_UPCOMING_EXPIRED_IN_LIST,
                getString(R.string.show_upcoming_and_expired_event_indicators)));
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK,
                getString(R.string.settings_show_global_items_lock)));
        
        settingsEntries.add(new ResetButtonSettingsEntryConfig(this, savedInstanceState));
        
        settingsListViewAdapter = new SettingsListViewAdapter(settingsEntries, getLifecycle());
    }
    
    // full destroy
    @Override
    public void onDestroy() {
        adaptiveBackgroundSettingsEntry = null;
        super.onDestroy();
    }
    
    public void notifyBgSelected() {
        adaptiveBackgroundSettingsEntry.notifyBackgroundUpdated();
    }
}