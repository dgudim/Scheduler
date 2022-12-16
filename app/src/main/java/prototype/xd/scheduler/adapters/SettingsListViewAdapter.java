package prototype.xd.scheduler.adapters;


import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.List;

import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;

public class SettingsListViewAdapter extends RecyclerView.Adapter<SettingsEntryConfig.SettingsViewHolder<? extends ViewBinding, ? extends SettingsEntryConfig>> {
    
    private boolean collapsed;
    private final List<? extends SettingsEntryConfig> settingsEntries;
    private final Lifecycle lifecycle;
    
    public SettingsListViewAdapter(@NonNull List<? extends SettingsEntryConfig> settingsEntries,
                                   @NonNull Lifecycle lifecycle, boolean collapsed) {
        this.settingsEntries = settingsEntries;
        this.lifecycle = lifecycle;
        this.collapsed = collapsed;
    }
    
    public SettingsListViewAdapter(@NonNull List<? extends SettingsEntryConfig> settingsEntries,
                                   @NonNull Lifecycle lifecycle) {
        this(settingsEntries, lifecycle, false);
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
        return collapsed ? 1 : settingsEntries.size();
    }
    
    public void setCollapsed(boolean collapsed) {
        boolean prevCollapsed = this.collapsed;
        this.collapsed = collapsed;
        if(prevCollapsed != collapsed) {
            if (prevCollapsed) {
                notifyItemRangeInserted(1, settingsEntries.size() - 1);
            } else {
                notifyItemRangeRemoved(1, settingsEntries.size() - 1);
            }
        }
    }
    
    public void toggleCollapsed() {
        setCollapsed(!collapsed);
    }
    
    public boolean isCollapsed() {
        return collapsed;
    }
}
