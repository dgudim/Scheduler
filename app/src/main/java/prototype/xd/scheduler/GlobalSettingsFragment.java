package prototype.xd.scheduler;

import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAYS_OF_WEEK;
import static prototype.xd.scheduler.utilities.DateManager.FIRST_DAY_OF_WEEK;
import static prototype.xd.scheduler.utilities.DateManager.getFirstDaysOfWeekLocal;
import static prototype.xd.scheduler.utilities.Logger.logException;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.entities.settings_entries.AdaptiveBackgroundSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.AppThemeSelectorEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.CompoundCustomizationEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.DoubleSliderSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.DropdownSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.ResetButtonSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SliderSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.GraphicsUtilities;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.Utilities;

public class GlobalSettingsFragment extends BaseListSettingsFragment<ConcatAdapter> {
    
    public static final String NAME = GlobalSettingsFragment.class.getSimpleName();
    
    private AdaptiveBackgroundSettingsEntryConfig adaptiveBackgroundSettingsEntry;
    
    final ActivityResultLauncher<Intent> pickBg =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::onBgSelected);
    
    // initial window creation
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Context context = requireContext();
        
        List<SettingsEntryConfig> settingsEntries = new ArrayList<>();
        
        adaptiveBackgroundSettingsEntry = new AdaptiveBackgroundSettingsEntryConfig(requireContext(), getLifecycle(),
                bgIndex -> Utilities.callImageFileChooser(pickBg));
        
        settingsEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_application_settings)));
        settingsEntries.add(new AppThemeSelectorEntryConfig());
        settingsEntries.add(new DropdownSettingsEntryConfig<>(R.string.first_weekday, getFirstDaysOfWeekLocal(), FIRST_DAYS_OF_WEEK, FIRST_DAY_OF_WEEK));
        
        
        settingsEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_lockscreen_appearance)));
        settingsEntries.add(adaptiveBackgroundSettingsEntry);
        settingsEntries.add(new CompoundCustomizationEntryConfig());
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.ITEM_FULL_WIDTH_LOCK, getString(R.string.settings_max_rWidth_lock)));
        settingsEntries.add(new SliderSettingsEntryConfig(Keys.LOCKSCREEN_VIEW_VERTICAL_BIAS,
                0, 100, 5, value -> {
            String baseString = context.getString(R.string.settings_event_vertical_bias, value) + "%";
            if (value == 0) {
                return baseString + " (" + context.getString(R.string.top) + ")";
            }
            if (value == 25) {
                return baseString + " (1/4)";
            }
            if (value == 50) {
                return baseString + " (" + context.getString(R.string.middle) + ")";
            }
            if (value == 75) {
                return baseString + " (3/4)";
            }
            if (value == 100) {
                return baseString + " (" + context.getString(R.string.bottom) + ")";
            }
            return baseString;
        }));
        
        
        settingsEntries.add(new TitleBarSettingsEntryConfig(getString(R.string.category_event_visibility)));
        settingsEntries.add(new DoubleSliderSettingsEntryConfig(context, R.string.settings_show_events,
                new SliderSettingsEntryConfig(Keys.UPCOMING_ITEMS_OFFSET,
                        0, Keys.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET, 1, false, R.plurals.settings_in_n_days),
                Keys.BG_COLOR.UPCOMING.defaultValue,
                new SliderSettingsEntryConfig(Keys.EXPIRED_ITEMS_OFFSET,
                        0, Keys.SETTINGS_MAX_EXPIRED_UPCOMING_ITEMS_OFFSET, 1, false, R.plurals.settings_after_n_days),
                Keys.BG_COLOR.EXPIRED.defaultValue));
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.MERGE_ENTRIES, getString(R.string.settings_merge_events)));
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.HIDE_EXPIRED_ENTRIES_BY_TIME, getString(R.string.settings_hide_expired_entries_by_time)));
        
        settingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.SHOW_UPCOMING_EXPIRED_IN_LIST, getString(R.string.show_upcoming_and_expired_event_indicators)));
        
        List<SettingsEntryConfig> globalSwitchSettingsEntries = new ArrayList<>();
        SettingsListViewAdapter globalSwitchSettingsListViewAdapter = new SettingsListViewAdapter(globalSwitchSettingsEntries, getLifecycle(),
                !Keys.SHOW_GLOBAL_ITEMS_LOCK.get());
        
        globalSwitchSettingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.SHOW_GLOBAL_ITEMS_LOCK, getString(R.string.settings_show_global_items_lock),
                (buttonView, isChecked) -> globalSwitchSettingsListViewAdapter.setCollapsed(!isChecked), false));
        
        globalSwitchSettingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.SHOW_GLOBAL_ITEMS_LABEL_LOCK, getString(R.string.settings_show_global_items_label_lock)));
        
        listViewAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false)
                .setStableIdMode(NO_STABLE_IDS).build(),
                new SettingsListViewAdapter(settingsEntries, getLifecycle()),
                globalSwitchSettingsListViewAdapter,
                new SettingsListViewAdapter(
                        Collections.singletonList(
                                new ResetButtonSettingsEntryConfig(this, savedInstanceState)), getLifecycle()));
        
    }
    
    // full destroy
    @Override
    public void onDestroy() {
        adaptiveBackgroundSettingsEntry = null;
        super.onDestroy();
    }
    
    public void onBgSelected(ActivityResult result) {
        
        Intent data = result.getData();
        if (data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        new Thread(() -> {
            try {
                
                InputStream stream = requireActivity().getContentResolver().openInputStream(uri);
                if (stream != null) {
                    GraphicsUtilities.fingerPrintAndSaveBitmap(BitmapFactory.decodeStream(stream),
                            new File(Keys.ROOT_DIR.get(), DateManager.WEEK_DAYS_ROOT.get(adaptiveBackgroundSettingsEntry.getLastClickedBgIndex()) + ".png"));
                    stream.close();
                    requireActivity().runOnUiThread(() -> adaptiveBackgroundSettingsEntry.notifyBackgroundUpdated());
                } else {
                    Logger.error(NAME, "Stream null for uri: " + uri.getPath());
                }
                
            } catch (Exception e) {
                logException(Thread.currentThread().getName(), e);
            }
        }, "LBCP thread").start();
    }
}