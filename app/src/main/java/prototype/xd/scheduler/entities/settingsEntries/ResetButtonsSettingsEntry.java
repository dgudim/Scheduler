package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.RESET_BUTTONS;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import prototype.xd.scheduler.R;

public class ResetButtonsSettingsEntry extends SettingsEntry {
    
    private final Fragment fragment;
    private final View view;
    private final Bundle savedInstanceState;
    
    public ResetButtonsSettingsEntry(Fragment fragment, View view, Bundle savedInstanceState) {
        super(R.layout.settings_reset_buttons_entry);
        this.fragment = fragment;
        this.view = view;
        this.savedInstanceState = savedInstanceState;
        entryType = RESET_BUTTONS;
    }
    
    @SuppressWarnings({"ApplySharedPref"})
    @Override
    protected View InitInnerViews(View convertView, ViewGroup viewGroup) {
        
        convertView.findViewById(R.id.resetSettingsButton).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(convertView.getContext());
            builder.setTitle("Сбросить настройки?");
        
            builder.setPositiveButton("Да", (dialog, which) -> {
                preferences.edit().clear().commit();
                fragment.onViewCreated(view, savedInstanceState);
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
        
            builder.show();
        });
        
        return super.InitInnerViews(convertView, viewGroup);
    }
}
