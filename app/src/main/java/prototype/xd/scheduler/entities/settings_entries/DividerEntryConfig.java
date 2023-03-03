package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.DIVIDER;

import androidx.annotation.NonNull;

import prototype.xd.scheduler.databinding.DividerBinding;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class DividerEntryConfig extends SettingsEntryConfig {
    @Override
    public int getRecyclerViewType() {
        return DIVIDER.ordinal();
    }
    
    static class DividerViewHolder extends SettingsEntryConfig.SettingsViewHolder<DividerBinding, DividerEntryConfig> {
        
        DividerViewHolder(@NonNull final ContextWrapper wrapper,
                          @NonNull final DividerBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(DividerEntryConfig config) {
            // nothing special required, this is just a divider
        }
    }
    
}
