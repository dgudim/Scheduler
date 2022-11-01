package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.ADAPTIVE_BACKGROUND_SETTINGS;
import static prototype.xd.scheduler.utilities.DateManager.AVAILABLE_DAYS;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.getFile;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.adapters.BackgroundImagesGridViewAdapter;
import prototype.xd.scheduler.utilities.Keys;

public class AdaptiveBackgroundSettingsEntry extends SettingsEntry {
    
    private final BackgroundImagesGridViewAdapter gridViewAdapter;
    
    public AdaptiveBackgroundSettingsEntry(SettingsFragment fragment) {
        super(R.layout.settings_adaptive_background_settings_entry);
        gridViewAdapter = new BackgroundImagesGridViewAdapter(fragment);
        entryType = ADAPTIVE_BACKGROUND_SETTINGS;
    }
    
    public void notifyBackgroundUpdated() {
        gridViewAdapter.notifyDataSetChanged();
    }
    
    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected View initInnerViews(View convertView, ViewGroup viewGroup) {
        convertView.findViewById(R.id.adaptive_bg_settings).setOnClickListener(v -> {
            final AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext(), R.style.FullScreenDialog);
            
            View view = LayoutInflater.from(convertView.getContext()).inflate(R.layout.background_images_grid_selection_view, viewGroup, false);
            addSwitchChangeListener(view.findViewById(R.id.adaptive_bg_switch), Keys.ADAPTIVE_BACKGROUND_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_BACKGROUND_ENABLED);
            
            view.findViewById(R.id.resetBgButton).setOnClickListener(view1 ->
                    displayConfirmationDialogue(view1.getContext(), R.string.delete_all_saved_backgrounds_prompt,
                            R.string.cancel, R.string.delete,
                            view2 -> {
                                for (String availableDay : AVAILABLE_DAYS) {
                                    getFile(availableDay + ".png").delete();
                                    getFile(availableDay + ".png_min.png").delete();
                                }
                                gridViewAdapter.notifyDataSetChanged();
                            }));
            
            GridView gridView = view.findViewById(R.id.grid_view);
            gridView.setNumColumns(2);
            gridView.setHorizontalSpacing(30);
            gridView.setVerticalSpacing(30);
            gridView.setAdapter(gridViewAdapter);
            
            alert.setView(view);
            alert.show();
        });
        return super.initInnerViews(convertView, viewGroup);
    }
}
