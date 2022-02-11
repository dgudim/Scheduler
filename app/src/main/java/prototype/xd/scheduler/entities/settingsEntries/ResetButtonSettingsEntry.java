package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.RESET_BUTTON;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayConfirmationDialogue;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import prototype.xd.scheduler.R;

public class ResetButtonSettingsEntry extends SettingsEntry {
    
    private final Fragment fragment;
    private final Bundle savedInstanceState;
    
    public ResetButtonSettingsEntry(Fragment fragment, Bundle savedInstanceState) {
        super(R.layout.settings_reset_button_entry);
        this.fragment = fragment;
        this.savedInstanceState = savedInstanceState;
        entryType = RESET_BUTTON;
    }
    
    @SuppressWarnings({"ApplySharedPref"})
    @Override
    protected View InitInnerViews(View convertView, ViewGroup viewGroup) {
        
        convertView.findViewById(R.id.resetSettingsButton).setOnClickListener(v ->
                displayConfirmationDialogue(v.getContext(),
                        R.string.reset_settings_prompt,
                        R.string.cancel, R.string.reset,
                        (view) -> {
                            preferences.edit().clear().commit();
                            fragment.onViewCreated(view, savedInstanceState);
                        }));
        
        return super.InitInnerViews(convertView, viewGroup);
    }
}
