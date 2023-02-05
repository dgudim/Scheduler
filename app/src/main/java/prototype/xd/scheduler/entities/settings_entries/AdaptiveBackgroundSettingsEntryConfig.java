package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.ADAPTIVE_BACKGROUND_SETTINGS;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayMessageDialog;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.GridView;

import androidx.annotation.NonNull;

import java.util.function.IntConsumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.PerDayBgGridViewAdapter;
import prototype.xd.scheduler.databinding.AdaptiveBackgroundSettingsEntryBinding;
import prototype.xd.scheduler.databinding.BgGridSelectionViewBinding;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.Utilities;

public class AdaptiveBackgroundSettingsEntryConfig extends SettingsEntryConfig {
    
    @NonNull
    private final PerDayBgGridViewAdapter gridViewAdapter;
    private Integer lastClickedBgIndex;
    
    public AdaptiveBackgroundSettingsEntryConfig(@NonNull final Context context,
                                                 @NonNull final IntConsumer bgSelectionClickedCallback) {
        gridViewAdapter = new PerDayBgGridViewAdapter(context, bgIndex -> {
            lastClickedBgIndex = bgIndex;
            bgSelectionClickedCallback.accept(bgIndex);
        });
    }
    
    @NonNull
    public Integer getLastClickedBgIndex() {
        return lastClickedBgIndex;
    }
    
    public void notifyBackgroundUpdated() {
        gridViewAdapter.notifyDataSetChanged();
    }
    
    @Override
    public int getRecyclerViewType() {
        return ADAPTIVE_BACKGROUND_SETTINGS.ordinal();
    }
    
    static class AdaptiveBackgroundViewHolder extends SettingsEntryConfig.SettingsViewHolder<AdaptiveBackgroundSettingsEntryBinding, AdaptiveBackgroundSettingsEntryConfig> {
        
        AdaptiveBackgroundViewHolder(@NonNull ContextWrapper wrapper, @NonNull AdaptiveBackgroundSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        void bind(@NonNull AdaptiveBackgroundSettingsEntryConfig config) {
            viewBinding.adaptiveBgSettings.setOnClickListener(v -> {
                
                BgGridSelectionViewBinding gridSelection = BgGridSelectionViewBinding.inflate(wrapper.getLayoutInflater());
                
                Utilities.setSwitchChangeListener(gridSelection.adaptiveBgSwitch,
                        Static.ADAPTIVE_BACKGROUND_ENABLED,
                        null);
                
                gridSelection.resetBgButton.setOnClickListener(view1 ->
                        displayMessageDialog(wrapper, builder -> {
                            builder.setTitle(R.string.delete_all_saved_backgrounds_prompt);
                            builder.setMessage(R.string.delete_all_saved_backgrounds_description);
                            builder.setIcon(R.drawable.ic_delete_50);
                            builder.setNegativeButton(R.string.cancel, null);
                            
                            builder.setPositiveButton(R.string.delete, (dialogInterface, whichButton) -> {
                                for (int dayIndex = 0; dayIndex < DateManager.BG_NAMES_ROOT.size() - 1; dayIndex++) {
                                    String bgName = DateManager.BG_NAMES_ROOT.get(dayIndex);
                                    getFile(bgName).delete();
                                    getFile(bgName + "_min.png").delete();
                                }
                                config.gridViewAdapter.notifyDataSetChanged();
                            });
                        }));
                
                GridView gridView = gridSelection.gridViewInclude.gridView;
                gridView.setNumColumns(2);
                gridView.setHorizontalSpacing(30);
                gridView.setVerticalSpacing(30);
                gridView.setAdapter(config.gridViewAdapter);
                
                wrapper.attachDialogToLifecycle(
                        new AlertDialog.Builder(v.getContext(), R.style.FullScreenDialog)
                                .setView(gridSelection.getRoot()).show(), null);
            });
        }
    }
}



