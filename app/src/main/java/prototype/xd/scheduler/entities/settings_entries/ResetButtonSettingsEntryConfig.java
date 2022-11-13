package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.RESET_BUTTON;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.ResetButtonSettingsEntryBinding;

public class ResetButtonSettingsEntryConfig implements SettingsEntryConfig {
    
    private final Fragment fragment;
    private final Bundle savedInstanceState;
    
    public ResetButtonSettingsEntryConfig(Fragment fragment, Bundle savedInstanceState) {
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
                    displayConfirmationDialogue(v.getContext(),
                            R.string.reset_settings_prompt,
                            R.string.cancel, R.string.reset,
                            view -> {
                                preferences.edit().clear().commit();
                                config.fragment.onViewCreated(view, config.savedInstanceState);
                            }));
        }
    }
}



