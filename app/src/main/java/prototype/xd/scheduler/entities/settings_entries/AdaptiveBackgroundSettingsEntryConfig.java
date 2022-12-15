package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.ADAPTIVE_BACKGROUND_SETTINGS;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import java.util.function.Consumer;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.BackgroundImagesGridViewAdapter;
import prototype.xd.scheduler.databinding.AdaptiveBackgroundSettingsEntryBinding;
import prototype.xd.scheduler.databinding.BgGridSelectionViewBinding;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Utilities;

public class AdaptiveBackgroundSettingsEntryConfig extends SettingsEntryConfig {
    
    @NonNull
    private final BackgroundImagesGridViewAdapter gridViewAdapter;
    @NonNull
    private final Lifecycle lifecycle;
    private Integer lastClickedBgIndex;
    
    public AdaptiveBackgroundSettingsEntryConfig(@NonNull final Context context,
                                                 @NonNull final Lifecycle lifecycle,
                                                 @NonNull final Consumer<Integer> bgSelectionClickedCallback) {
        gridViewAdapter = new BackgroundImagesGridViewAdapter(context, bgIndex -> {
            lastClickedBgIndex = bgIndex;
            bgSelectionClickedCallback.accept(bgIndex);
        });
        this.lifecycle = lifecycle;
    }
    
    public Integer getLastClickedBgIndex() {
        return lastClickedBgIndex;
    }
    
    public void notifyBackgroundUpdated() {
        gridViewAdapter.notifyDataSetChanged();
    }
    
    @Override
    public int getType() {
        return ADAPTIVE_BACKGROUND_SETTINGS.ordinal();
    }
    
    static class AdaptiveBackgroundViewHolder extends SettingsEntryConfig.SettingsViewHolder<AdaptiveBackgroundSettingsEntryBinding, AdaptiveBackgroundSettingsEntryConfig> {
        
        AdaptiveBackgroundViewHolder(AdaptiveBackgroundSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        void bind(AdaptiveBackgroundSettingsEntryConfig config) {
            viewBinding.adaptiveBgSettings.setOnClickListener(v -> {
                final AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext(), R.style.FullScreenDialog);
                
                BgGridSelectionViewBinding gridSelection = BgGridSelectionViewBinding.inflate(LayoutInflater.from(context));
                
                Utilities.setSwitchChangeListener(gridSelection.adaptiveBgSwitch, Keys.ADAPTIVE_BACKGROUND_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_BACKGROUND_ENABLED);
                
                gridSelection.resetBgButton.setOnClickListener(view1 ->
                        displayConfirmationDialogue(view1.getContext(), config.lifecycle,
                                R.string.delete_all_saved_backgrounds_prompt,
                                R.string.cancel, R.string.delete,
                                view2 -> {
                                    for (String availableDay : Keys.WEEK_DAYS) {
                                        getFile(availableDay + ".png").delete();
                                        getFile(availableDay + ".png_min.png").delete();
                                    }
                                    config.gridViewAdapter.notifyDataSetChanged();
                                }));
                
                GridView gridView = gridSelection.gridViewInclude.gridView;
                gridView.setNumColumns(2);
                gridView.setHorizontalSpacing(30);
                gridView.setVerticalSpacing(30);
                gridView.setAdapter(config.gridViewAdapter);
                
                alert.setView(gridSelection.getRoot());
                alert.show();
            });
        }
    }
}



