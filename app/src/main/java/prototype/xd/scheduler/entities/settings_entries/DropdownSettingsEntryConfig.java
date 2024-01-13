package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.DROPDOWN;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.List;

import prototype.xd.scheduler.databinding.DropdownSettingsEntryBinding;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public class DropdownSettingsEntryConfig<T> extends SettingsEntryConfig {
    
    @StringRes
    private final int hintId;
    @NonNull
    private final String[] displayItems;
    @NonNull
    private final List<T> items;
    @NonNull
    private final Static.DefaultedValue<T> value;
    
    public DropdownSettingsEntryConfig(@StringRes int hintId,
                                       @NonNull final List<String> displayItems,
                                       @NonNull final List<T> items,
                                       @NonNull final Static.DefaultedValue<T> value) {
        
        if (displayItems.size() != items.size()) {
            throw new IllegalArgumentException("displayItems.size != items.size");
        }
        
        this.hintId = hintId;
        this.items = items;
        this.displayItems = displayItems.toArray(new String[0]);
        this.value = value;
    }
    
    private int getValueIndex() {
        return items.indexOf(value.get());
    }
    
    private void putValueIndex(int position) {
        value.put(items.get(position));
    }
    
    @Override
    public int getRecyclerViewType() {
        return DROPDOWN.ordinal();
    }
    
    static class ViewHolder extends SettingsEntryConfig.SettingsViewHolder<DropdownSettingsEntryBinding, DropdownSettingsEntryConfig<?>> {
        
        ViewHolder(@NonNull ContextWrapper wrapper, @NonNull DropdownSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(@NonNull DropdownSettingsEntryConfig<?> config) {
            binding.textInputLayout.setHint(config.hintId);
            binding.dropdownSpinner.setSimpleItems(config.displayItems);
            binding.dropdownSpinner.setSelectedItem(config.getValueIndex());
            binding.dropdownSpinner.setOnItemClickListener((parent, view, position, id) -> config.putValueIndex(position));
        }
    }
    
}
