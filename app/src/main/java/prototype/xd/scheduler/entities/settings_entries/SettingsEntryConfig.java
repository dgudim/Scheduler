package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.AdaptiveBackgroundSettingsEntryConfig.AdaptiveBackgroundViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.AppThemeSelectorEntryConfig.AppThemeSelectorViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.CalendarAccountSettingsEntryConfig.CalendarAccountViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.CalendarSettingsEntryConfig.CalendarViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.CompoundCustomizationEntryConfig.CompoundCustomizationViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.SeekBarSettingsEntryConfig.SeekBarViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig.SwitchViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig.TitleBarViewHolder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import prototype.xd.scheduler.databinding.AdaptiveBackgroundSettingsEntryBinding;
import prototype.xd.scheduler.databinding.AppThemeSelectorSettingsEntryBinding;
import prototype.xd.scheduler.databinding.CalendarAccountSettingsEntryBinding;
import prototype.xd.scheduler.databinding.CalendarSettingsEntryBinding;
import prototype.xd.scheduler.databinding.CompoundCustomizationSettingsEntryBinding;
import prototype.xd.scheduler.databinding.DropdownSettingsEntryBinding;
import prototype.xd.scheduler.databinding.ResetButtonSettingsEntryBinding;
import prototype.xd.scheduler.databinding.SeekbarSettingsEntryBinding;
import prototype.xd.scheduler.databinding.SwitchSettingsEntryBinding;
import prototype.xd.scheduler.databinding.TitleSettingsEntryBinding;
import prototype.xd.scheduler.entities.RecycleViewEntry;
import prototype.xd.scheduler.entities.settings_entries.DropdownSettingsEntryConfig.DropdownViewHolder;
import prototype.xd.scheduler.entities.settings_entries.ResetButtonSettingsEntryConfig.ResetButtonViewHolder;



enum SettingsEntryType {
    CALENDAR_ACCOUNT, CALENDAR, COMPOUND_CUSTOMIZATION,
    RESET_BUTTON, SEEK_BAR, SWITCH, DROPDOWN, TITLE_BAR, ADAPTIVE_BACKGROUND_SETTINGS, APP_THEME_SELECTOR
}

public abstract class SettingsEntryConfig extends RecycleViewEntry {
    
    public static SettingsViewHolder<?, ? extends SettingsEntryConfig> createViewHolder(ViewGroup parent, int viewType, Lifecycle lifecycle) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        // we should make sure to return appropriate holders, otherwise it will crash in @onBindViewHolder
        switch (SettingsEntryType.values()[viewType]) {
            case CALENDAR_ACCOUNT:
                return new CalendarAccountViewHolder(CalendarAccountSettingsEntryBinding.inflate(inflater, parent, false));
            case CALENDAR:
                return new CalendarViewHolder(CalendarSettingsEntryBinding.inflate(inflater, parent, false));
            case COMPOUND_CUSTOMIZATION:
                return new CompoundCustomizationViewHolder(
                        CompoundCustomizationSettingsEntryBinding.inflate(inflater, parent, false), lifecycle);
            case RESET_BUTTON:
                return new ResetButtonViewHolder(ResetButtonSettingsEntryBinding.inflate(inflater, parent, false));
            case SEEK_BAR:
                return new SeekBarViewHolder(SeekbarSettingsEntryBinding.inflate(inflater, parent, false));
            case SWITCH:
                return new SwitchViewHolder(SwitchSettingsEntryBinding.inflate(inflater, parent, false));
            case DROPDOWN:
                return new DropdownViewHolder(DropdownSettingsEntryBinding.inflate(inflater, parent, false));
            case ADAPTIVE_BACKGROUND_SETTINGS:
                return new AdaptiveBackgroundViewHolder(AdaptiveBackgroundSettingsEntryBinding.inflate(inflater, parent, false));
            case APP_THEME_SELECTOR:
                return new AppThemeSelectorViewHolder(AppThemeSelectorSettingsEntryBinding.inflate(inflater, parent, false));
            case TITLE_BAR:
                return new TitleBarViewHolder(TitleSettingsEntryBinding.inflate(inflater, parent, false));
            default:
                throw new IllegalArgumentException("Can't create viewHolder for " + SettingsEntryType.values()[viewType]);
        }
    }
    
    public abstract static class SettingsViewHolder<V extends ViewBinding, S extends SettingsEntryConfig> extends RecyclerView.ViewHolder {
        
        protected V viewBinding;
        protected Context context;
        
        SettingsViewHolder(V viewBinding) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
            context = viewBinding.getRoot().getContext();
        }
        
        abstract void bind(S config);
        
        @SuppressWarnings("unchecked")
        public void uncheckedBind(SettingsEntryConfig settingsEntryConfig) {
            bind((S) settingsEntryConfig);
        }
    }
    
}

