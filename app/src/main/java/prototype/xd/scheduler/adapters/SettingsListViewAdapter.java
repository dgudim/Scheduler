package prototype.xd.scheduler.adapters;


import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.List;

import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;

public class SettingsListViewAdapter extends RecyclerView.Adapter<SettingsEntryConfig.SettingsViewHolder<? extends ViewBinding, ? extends SettingsEntryConfig>> {
    
    private final List<? extends SettingsEntryConfig> settingsEntries;
    
    public SettingsListViewAdapter(List<? extends SettingsEntryConfig> settingsEntries) {
        this.settingsEntries = settingsEntries;
    }
    
    @NonNull
    @Override
    public SettingsEntryConfig.SettingsViewHolder<? extends ViewBinding, ? extends SettingsEntryConfig> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return SettingsEntryConfig.createViewHolder(parent, viewType);
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
