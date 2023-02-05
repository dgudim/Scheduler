package prototype.xd.scheduler.entities.settings_entries;

import static android.content.DialogInterface.OnClickListener;
import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.RESET_BUTTON;

import androidx.annotation.NonNull;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.ResetButtonSettingsEntryBinding;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.utilities.DialogUtilities;

public class ResetButtonSettingsEntryConfig extends SettingsEntryConfig {
    
    @NonNull
    private final OnClickListener onResetClickListener;
    
    public ResetButtonSettingsEntryConfig(@NonNull final OnClickListener onResetClickListener) {
        this.onResetClickListener = onResetClickListener;
    }
    
    @Override
    public int getRecyclerViewType() {
        return RESET_BUTTON.ordinal();
    }
    
    static class ResetButtonViewHolder extends SettingsEntryConfig.SettingsViewHolder<ResetButtonSettingsEntryBinding, ResetButtonSettingsEntryConfig> {
        
        ResetButtonViewHolder(@NonNull ContextWrapper wrapper, @NonNull ResetButtonSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(@NonNull ResetButtonSettingsEntryConfig config) {
            viewBinding.resetSettingsButton.setOnClickListener(v ->
                    DialogUtilities.displayMessageDialog(wrapper, builder -> {
                        builder.setTitle(R.string.reset_settings_prompt);
                        builder.setMessage(R.string.reset_settings_description);
                        builder.setIcon(R.drawable.ic_clear_all_24);
                        builder.setNegativeButton(R.string.cancel, null);
                        builder.setPositiveButton(R.string.reset, config.onResetClickListener);
                    }));
        }
    }
}



