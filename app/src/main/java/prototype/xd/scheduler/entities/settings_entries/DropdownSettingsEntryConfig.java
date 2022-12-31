package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.DROPDOWN;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.List;

import prototype.xd.scheduler.databinding.DropdownSettingsEntryBinding;
import prototype.xd.scheduler.utilities.Keys;

public class DropdownSettingsEntryConfig<T> extends SettingsEntryConfig {
    
    @StringRes
    private final int hintId;
    private final String[] displayItems;
    private final List<T> items;
    private final Keys.DefaultedValue<T> value;
    
    public DropdownSettingsEntryConfig(@StringRes int hintId,
                                       @NonNull final String[] displayItems,
                                       @NonNull final List<T> items,
                                       @NonNull final Keys.DefaultedValue<T> value) {
        this.hintId = hintId;
        this.displayItems = displayItems;
        this.items = items;
        if(displayItems.length != items.size()) {
            throw new IllegalArgumentException("displayItems.size != items.size");
        }
        this.value = value;
    }
    
    private int getValueIndex() {
        return items.indexOf(value.get());
    }
    
    private void putValueIndex(int position) {
        value.put(items.get(position));
    }
    
    @Override
    public int getType() {
        return DROPDOWN.ordinal();
    }
    
    static class DropdownViewHolder extends SettingsEntryConfig.SettingsViewHolder<DropdownSettingsEntryBinding, DropdownSettingsEntryConfig<?>> {
        
        DropdownViewHolder(DropdownSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @Override
        void bind(DropdownSettingsEntryConfig<?> config) {
            viewBinding.textInputLayout.setHint(config.hintId);
            viewBinding.dropdownSpinner.setSimpleItems(config.displayItems);
            viewBinding.dropdownSpinner.setSelectedItem(config.getValueIndex());
            viewBinding.dropdownSpinner.setOnItemClickListener((parent, view, position, id) -> config.putValueIndex(position));
        }
    }
    
}
