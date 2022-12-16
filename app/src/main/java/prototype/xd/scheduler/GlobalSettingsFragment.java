package prototype.xd.scheduler;

import static android.util.Log.ERROR;
import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.Utilities.getRootDir;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
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
import prototype.xd.scheduler.entities.settings_entries.ResetButtonSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SeekBarSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig;
import prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig;
import prototype.xd.scheduler.utilities.BitmapUtilities;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Utilities;

public class GlobalSettingsFragment extends BaseSettingsFragment<ConcatAdapter> {
    
    private AdaptiveBackgroundSettingsEntryConfig adaptiveBackgroundSettingsEntry;
    
    ActivityResultLauncher<Intent> pickBg =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::onBgSelected);
    
    // initial window creation
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
        
        List<SettingsEntryConfig> settingsEntries = new ArrayList<>();
        
        adaptiveBackgroundSettingsEntry = new AdaptiveBackgroundSettingsEntryConfig(requireContext(), getLifecycle(), this::selectBackground);
        
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
        
        List<SettingsEntryConfig> globalSwitchSettingsEntries = new ArrayList<>();
        SettingsListViewAdapter globalSwitchSettingsListViewAdapter = new SettingsListViewAdapter(globalSwitchSettingsEntries, getLifecycle(),
                !preferences.getBoolean(Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK));
        
        globalSwitchSettingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.SHOW_GLOBAL_ITEMS_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LOCK,
                getString(R.string.settings_show_global_items_lock),
                (buttonView, isChecked) -> globalSwitchSettingsListViewAdapter.setCollapsed(!isChecked)));
        
        globalSwitchSettingsEntries.add(new SwitchSettingsEntryConfig(
                Keys.SHOW_GLOBAL_ITEMS_LABEL_LOCK, Keys.SETTINGS_DEFAULT_SHOW_GLOBAL_ITEMS_LABEL_LOCK,
                getString(R.string.settings_show_global_items_label_lock)));
        
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
    
    public void selectBackground(Integer bgIndex) {
        Utilities.callImageFileChooser(pickBg);
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
                    BitmapUtilities.fingerPrintAndSaveBitmap(BitmapFactory.decodeStream(stream),
                            new File(getRootDir(), Keys.WEEK_DAYS.get(adaptiveBackgroundSettingsEntry.getLastClickedBgIndex()) + ".png"));
                    stream.close();
                    requireActivity().runOnUiThread(() -> adaptiveBackgroundSettingsEntry.notifyBackgroundUpdated());
                } else {
                    log(ERROR, "BackgroundImagesGridViewAdapter", "stream null for uri: " + uri.getPath());
                }
                
            } catch (Exception e) {
                logException(Thread.currentThread().getName(), e);
            }
        }, "LBCP thread").start();
    }
}