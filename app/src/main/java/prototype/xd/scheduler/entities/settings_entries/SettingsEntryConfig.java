package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.AdaptiveBackgroundSettingsEntryConfig.AdaptiveBackgroundViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.AppThemeSelectorEntryConfig.AppThemeSelectorViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.CalendarAccountSettingsEntryConfig.CalendarAccountViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.CalendarSettingsEntryConfig.CalendarViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.CompoundCustomizationEntryConfig.CompoundCustomizationViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.SliderSettingsEntryConfig.SeekBarViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.SwitchSettingsEntryConfig.SwitchViewHolder;
import static prototype.xd.scheduler.entities.settings_entries.TitleBarSettingsEntryConfig.TitleBarViewHolder;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import prototype.xd.scheduler.databinding.AdaptiveBackgroundSettingsEntryBinding;
import prototype.xd.scheduler.databinding.AppThemeSelectorSettingsEntryBinding;
import prototype.xd.scheduler.databinding.CalendarAccountSettingsEntryBinding;
import prototype.xd.scheduler.databinding.CalendarSettingsEntryBinding;
import prototype.xd.scheduler.databinding.CompoundCustomizationSettingsEntryBinding;
import prototype.xd.scheduler.databinding.DividerBinding;
import prototype.xd.scheduler.databinding.DoubleSliderSettingsEntryBinding;
import prototype.xd.scheduler.databinding.DropdownSettingsEntryBinding;
import prototype.xd.scheduler.databinding.ResetButtonSettingsEntryBinding;
import prototype.xd.scheduler.databinding.SliderSettingsEntryBinding;
import prototype.xd.scheduler.databinding.SwitchSettingsEntryBinding;
import prototype.xd.scheduler.databinding.TitleSettingsEntryBinding;
import prototype.xd.scheduler.entities.RecycleViewEntry;
import prototype.xd.scheduler.entities.settings_entries.DividerEntryConfig.DividerViewHolder;
import prototype.xd.scheduler.entities.settings_entries.DoubleSliderSettingsEntryConfig.DoubleSeekBarViewHolder;
import prototype.xd.scheduler.entities.settings_entries.DropdownSettingsEntryConfig.DropdownViewHolder;
import prototype.xd.scheduler.entities.settings_entries.ResetButtonSettingsEntryConfig.ResetButtonViewHolder;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;


enum SettingsEntryType {
    CALENDAR_ACCOUNT, CALENDAR, COMPOUND_CUSTOMIZATION,
    DIVIDER,
    RESET_BUTTON, SLIDER, DOUBLE_SLIDER, SWITCH, DROPDOWN, TITLE_BAR, ADAPTIVE_BACKGROUND_SETTINGS, APP_THEME_SELECTOR
}

public abstract class SettingsEntryConfig extends RecycleViewEntry {
    
    @NonNull
    public static SettingsViewHolder<?, ? extends SettingsEntryConfig> createViewHolder(@NonNull ContextWrapper wrapper,
                                                                                        @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = wrapper.getLayoutInflater();
        // we should make sure to return appropriate holders, otherwise it will crash in @onBindViewHolder
        switch (SettingsEntryType.values()[viewType]) {
            case CALENDAR_ACCOUNT:
                return new CalendarAccountViewHolder(wrapper, CalendarAccountSettingsEntryBinding.inflate(inflater, parent, false));
            case CALENDAR:
                return new CalendarViewHolder(wrapper, CalendarSettingsEntryBinding.inflate(inflater, parent, false));
            case COMPOUND_CUSTOMIZATION:
                return new CompoundCustomizationViewHolder(wrapper,
                        CompoundCustomizationSettingsEntryBinding.inflate(inflater, parent, false));
            case RESET_BUTTON:
                return new ResetButtonViewHolder(wrapper, ResetButtonSettingsEntryBinding.inflate(inflater, parent, false));
            case SLIDER:
                return new SeekBarViewHolder(wrapper, SliderSettingsEntryBinding.inflate(inflater, parent, false));
            case DOUBLE_SLIDER:
                return new DoubleSeekBarViewHolder(wrapper, DoubleSliderSettingsEntryBinding.inflate(inflater, parent, false));
            case SWITCH:
                return new SwitchViewHolder(wrapper, SwitchSettingsEntryBinding.inflate(inflater, parent, false));
            case DROPDOWN:
                return new DropdownViewHolder(wrapper, DropdownSettingsEntryBinding.inflate(inflater, parent, false));
            case ADAPTIVE_BACKGROUND_SETTINGS:
                return new AdaptiveBackgroundViewHolder(wrapper, AdaptiveBackgroundSettingsEntryBinding.inflate(inflater, parent, false));
            case APP_THEME_SELECTOR:
                return new AppThemeSelectorViewHolder(wrapper, AppThemeSelectorSettingsEntryBinding.inflate(inflater, parent, false));
            case TITLE_BAR:
                return new TitleBarViewHolder(wrapper, TitleSettingsEntryBinding.inflate(inflater, parent, false));
            case DIVIDER:
                return new DividerViewHolder(wrapper, DividerBinding.inflate(inflater, parent, false));
            default:
                throw new IllegalArgumentException("Can't create viewHolder for " + SettingsEntryType.values()[viewType]);
        }
    }
    
    public abstract static class SettingsViewHolder<V extends ViewBinding, S extends SettingsEntryConfig> extends RecyclerView.ViewHolder {
        
        @NonNull
        protected final V viewBinding;
        @NonNull
        protected final ContextWrapper wrapper;
        
        SettingsViewHolder(@NonNull final ContextWrapper wrapper,
                           @NonNull final V viewBinding) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
            this.wrapper = wrapper;
        }
        
        abstract void bind(S config);
        
        @SuppressWarnings("unchecked")
        @MainThread
        public void uncheckedBind(@NonNull final SettingsEntryConfig settingsEntryConfig) {
            bind((S) settingsEntryConfig);
        }
    }
    
}

