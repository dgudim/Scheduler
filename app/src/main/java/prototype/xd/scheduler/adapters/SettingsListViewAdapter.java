package prototype.xd.scheduler.adapters;


import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.List;

import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;

public class SettingsListViewAdapter extends RecyclerView.Adapter<SettingsEntryConfig.SettingsViewHolder<? extends ViewBinding, ? extends SettingsEntryConfig>> {
    
    private final List<? extends SettingsEntryConfig> settingsEntries;
    private final Lifecycle lifecycle;
    
    public SettingsListViewAdapter(@NonNull List<? extends SettingsEntryConfig> settingsEntries,
                                   @NonNull Lifecycle lifecycle) {
        this.settingsEntries = settingsEntries;
        this.lifecycle = lifecycle;
    }
    
    @NonNull
    @Override
    public SettingsEntryConfig.SettingsViewHolder<? extends ViewBinding, ? extends SettingsEntryConfig> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return SettingsEntryConfig.createViewHolder(parent, viewType, lifecycle);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SettingsEntryConfig.SettingsViewHolder<? extends ViewBinding, ? extends SettingsEntryConfig> holder, int position) {
        holder.uncheckedBind(settingsEntries.get(position));
    }
    
    @Override
    public int getItemViewType(int i) {
        return settingsEntries.get(i).getType();
    }
    
    @Override
    public int getItemCount() {
        return settingsEntries.size();
    }
}
