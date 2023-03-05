package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.DIVIDER;

import androidx.annotation.NonNull;

import prototype.xd.scheduler.databinding.DividerSettingsEntryBinding;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class DividerSettingsEntryConfig extends SettingsEntryConfig {
    @Override
    public int getRecyclerViewType() {
        return DIVIDER.ordinal();
    }
    
    static class ViewHolder extends SettingsEntryConfig.SettingsViewHolder<DividerSettingsEntryBinding, DividerSettingsEntryConfig> {
        
        ViewHolder(@NonNull final ContextWrapper wrapper,
                   @NonNull final DividerSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(DividerSettingsEntryConfig config) {
            // nothing special required, this is just a divider
        }
    }
    
}
