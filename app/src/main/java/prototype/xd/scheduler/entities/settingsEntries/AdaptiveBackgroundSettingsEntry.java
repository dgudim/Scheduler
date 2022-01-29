package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.ADAPTIVE_BACKGROUND_SETTINGS;
import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import java.io.File;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.adapters.BackgroundImagesGridViewAdapter;
import prototype.xd.scheduler.utilities.Keys;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class AdaptiveBackgroundSettingsEntry extends SettingsEntry{
    
    private final Context context;
    private final LayoutInflater inflater;
    private final BackgroundImagesGridViewAdapter gridViewAdapter;
    
    private final ViewGroup root;
    
    public AdaptiveBackgroundSettingsEntry(SettingsFragment fragment) {
        super(R.layout.settings_adaptive_background_settings_entry);
        context = fragment.context;
        inflater = LayoutInflater.from(context);
        root = fragment.rootViewGroup;
        gridViewAdapter = new BackgroundImagesGridViewAdapter(fragment);
        entryType = ADAPTIVE_BACKGROUND_SETTINGS;
    }
    
    public void notifyBackgroundUpdated(){
        gridViewAdapter.notifyDataSetChanged();
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        rootView.findViewById(R.id.adaptive_bg_settings).setOnClickListener(v -> {
            final AlertDialog.Builder alert = new AlertDialog.Builder(context);
    
            View view = inflater.inflate(R.layout.background_images_grid_selection_view, root, false);
            addSwitchChangeListener(view.findViewById(R.id.adaptive_bg_switch), Keys.ADAPTIVE_BACKGROUND_ENABLED, Keys.SETTINGS_DEFAULT_ADAPTIVE_BACKGROUND_ENABLED);
    
            view.findViewById(R.id.resetBgButton).setOnClickListener(view1 -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.remove_all_saved_backgrounds_prompt);
        
                builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                    for (String availableDay : availableDays) {
                        new File(rootDir, availableDay + ".png").delete();
                        new File(rootDir, availableDay + ".png_min.png").delete();
                    }
                    gridViewAdapter.notifyDataSetChanged();
                });
                builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                
                builder.show();
            });
            
            GridView gridView = view.findViewById(R.id.grid_view);
            gridView.setNumColumns(2);
            gridView.setHorizontalSpacing(5);
            gridView.setVerticalSpacing(5);
            gridView.setAdapter(gridViewAdapter);
    
            alert.setView(view);
            alert.show();
        });
        return super.InitInnerViews(rootView);
    }
}
