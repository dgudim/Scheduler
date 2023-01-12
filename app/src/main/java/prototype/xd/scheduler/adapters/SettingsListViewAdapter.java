package prototype.xd.scheduler.adapters;


import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.List;

import prototype.xd.scheduler.entities.settings_entries.SettingsEntryConfig;
import prototype.xd.scheduler.utilities.ContextWrapper;

/**
 * List adapter class for displaying settings in settings screen
 */
public class SettingsListViewAdapter extends RecyclerView.Adapter<SettingsEntryConfig.SettingsViewHolder<? extends ViewBinding, ? extends SettingsEntryConfig>> {
    
    // if set to true the adapter will only display first entry effectively collapsing the list
    private boolean collapsed;
    private final List<? extends SettingsEntryConfig> settingsEntries;
    private final ContextWrapper wrapper;
    
    public SettingsListViewAdapter(@NonNull ContextWrapper wrapper,
                                   @NonNull List<? extends SettingsEntryConfig> settingsEntries,
                                   boolean collapsed) {
        this.settingsEntries = settingsEntries;
        this.wrapper = wrapper;
        this.collapsed = collapsed;
    }
    
    public SettingsListViewAdapter(@NonNull ContextWrapper wrapper,
                                   @NonNull List<? extends SettingsEntryConfig> settingsEntries) {
        this(wrapper, settingsEntries, false);
    }
    
    @NonNull
    @Override
    public SettingsEntryConfig.SettingsViewHolder<? extends ViewBinding, ? extends SettingsEntryConfig> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return SettingsEntryConfig.createViewHolder(wrapper, parent, viewType);
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
    
    /**
     * Set new collapsed state and notify the adapter
     *
     * @param newCollapsed new state
     */
    public void setCollapsed(boolean newCollapsed) {
        if (collapsed != newCollapsed) {
            if (newCollapsed) {
                notifyItemRangeRemoved(1, settingsEntries.size() - 1);
            } else {
                notifyItemRangeInserted(1, settingsEntries.size() - 1);
            }
            collapsed = newCollapsed;
        }
    }
    
    /**
     * Toggle collapsed state (true -> false, false -> true)
     */
    public void toggleCollapsed() {
        setCollapsed(!collapsed);
    }
    
    /**
     * Get collapsed state
     *
     * @return whether the list is collapsed or not
     */
    public boolean isCollapsed() {
        return collapsed;
    }
}
