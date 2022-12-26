package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.RESET_BUTTON;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayConfirmationDialogue;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.ResetButtonSettingsEntryBinding;
import prototype.xd.scheduler.utilities.Keys;

public class ResetButtonSettingsEntryConfig extends SettingsEntryConfig {
    
    private final Fragment fragment;
    private final Bundle savedInstanceState;
    
    public ResetButtonSettingsEntryConfig(@NonNull final Fragment fragment,
                                          @Nullable final Bundle savedInstanceState) {
        this.fragment = fragment;
        this.savedInstanceState = savedInstanceState;
    }
    
    @Override
    public int getType() {
        return RESET_BUTTON.ordinal();
    }
    
    static class ResetButtonViewHolder extends SettingsEntryConfig.SettingsViewHolder<ResetButtonSettingsEntryBinding, ResetButtonSettingsEntryConfig> {
        
        ResetButtonViewHolder(ResetButtonSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @Override
        void bind(ResetButtonSettingsEntryConfig config) {
            viewBinding.resetSettingsButton.setOnClickListener(v ->
                    displayConfirmationDialogue(v.getContext(), config.fragment.getLifecycle(),
                            R.string.reset_settings_prompt, R.string.reset_settings_description,
                            R.string.cancel, R.string.reset,
                            view -> {
                                Keys.clearAll();
                                config.fragment.onViewCreated(view, config.savedInstanceState);
                            }));
        }
    }
}



