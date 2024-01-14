package prototype.xd.scheduler.entities.settings_entries;


import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import prototype.xd.scheduler.adapters.BindingViewHolder;
import prototype.xd.scheduler.databinding.AdaptiveBackgroundSettingsEntryBinding;
import prototype.xd.scheduler.databinding.AppThemeSelectorSettingsEntryBinding;
import prototype.xd.scheduler.databinding.CalendarAccountSettingsEntryBinding;
import prototype.xd.scheduler.databinding.CalendarSettingsEntryBinding;
import prototype.xd.scheduler.databinding.CompoundCustomizationSettingsEntryBinding;
import prototype.xd.scheduler.databinding.DividerSettingsEntryBinding;
import prototype.xd.scheduler.databinding.DoubleSliderSettingsEntryBinding;
import prototype.xd.scheduler.databinding.DropdownSettingsEntryBinding;
import prototype.xd.scheduler.databinding.ImportExportSettingsEntryBinding;
import prototype.xd.scheduler.databinding.ResetButtonSettingsEntryBinding;
import prototype.xd.scheduler.databinding.SliderSettingsEntryBinding;
import prototype.xd.scheduler.databinding.SwitchSettingsEntryBinding;
import prototype.xd.scheduler.databinding.TitleSettingsEntryBinding;
import prototype.xd.scheduler.entities.RecycleViewEntry;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;


enum SettingsEntryType {
    CALENDAR_ACCOUNT, CALENDAR, COMPOUND_CUSTOMIZATION,
    DIVIDER,
    RESET_BUTTON, SLIDER, DOUBLE_SLIDER, SWITCH, DROPDOWN, TITLE_BAR, ADAPTIVE_BACKGROUND_SETTINGS, APP_THEME_SELECTOR,
    IMPORT_EXPORT_SETTINGS
}

public abstract class SettingsEntryConfig extends RecycleViewEntry {
    
    @NonNull
    public static SettingsViewHolder<?, ? extends SettingsEntryConfig> createViewHolder(@NonNull ContextWrapper wrapper,
                                                                                        @NonNull ViewGroup parent,
                                                                                        int viewType) {
        LayoutInflater inflater = wrapper.getLayoutInflater();
        // we should make sure to return appropriate holders, otherwise it will crash in @onBindViewHolder
        return switch (SettingsEntryType.values()[viewType]) {
            case CALENDAR_ACCOUNT ->
                    new CalendarAccountSettingsEntryConfig.ViewHolder(wrapper, CalendarAccountSettingsEntryBinding.inflate(inflater, parent, false));
            case CALENDAR ->
                    new CalendarSettingsEntryConfig.ViewHolder(wrapper, CalendarSettingsEntryBinding.inflate(inflater, parent, false));
            case COMPOUND_CUSTOMIZATION ->
                    new CompoundCustomizationSettingsEntryConfig.ViewHolder(wrapper,
                            CompoundCustomizationSettingsEntryBinding.inflate(inflater, parent, false));
            case RESET_BUTTON ->
                    new ResetButtonSettingsEntryConfig.ViewHolder(wrapper, ResetButtonSettingsEntryBinding.inflate(inflater, parent, false));
            case SLIDER ->
                    new SliderSettingsEntryConfig.ViewHolder(wrapper, SliderSettingsEntryBinding.inflate(inflater, parent, false));
            case DOUBLE_SLIDER ->
                    new DoubleSliderSettingsEntryConfig.ViewHolder(wrapper, DoubleSliderSettingsEntryBinding.inflate(inflater, parent, false));
            case SWITCH ->
                    new SwitchSettingsEntryConfig.ViewHolder(wrapper, SwitchSettingsEntryBinding.inflate(inflater, parent, false));
            case DROPDOWN ->
                    new DropdownSettingsEntryConfig.ViewHolder(wrapper, DropdownSettingsEntryBinding.inflate(inflater, parent, false));
            case ADAPTIVE_BACKGROUND_SETTINGS ->
                    new AdaptiveBackgroundSettingsEntryConfig.ViewHolder(wrapper, AdaptiveBackgroundSettingsEntryBinding.inflate(inflater, parent, false));
            case APP_THEME_SELECTOR ->
                    new AppThemeSelectorEntryConfig.ViewHolder(wrapper, AppThemeSelectorSettingsEntryBinding.inflate(inflater, parent, false));
            case IMPORT_EXPORT_SETTINGS ->
                    new ImportExportSettingsEntryConfig.ViewHolder(wrapper, ImportExportSettingsEntryBinding.inflate(inflater, parent, false));
            case TITLE_BAR ->
                    new TitleBarSettingsEntryConfig.ViewHolder(wrapper, TitleSettingsEntryBinding.inflate(inflater, parent, false));
            case DIVIDER ->
                    new DividerSettingsEntryConfig.ViewHolder(wrapper, DividerSettingsEntryBinding.inflate(inflater, parent, false));
        };
    }
    
    public abstract static class SettingsViewHolder<V extends ViewBinding, S extends SettingsEntryConfig> extends BindingViewHolder<V> {
        
        @NonNull
        protected final ContextWrapper wrapper;
        
        SettingsViewHolder(@NonNull final ContextWrapper wrapper,
                           @NonNull final V viewBinding) {
            super(viewBinding);
            this.wrapper = wrapper;
        }
        
        abstract void bind(S config);
        
        @SuppressWarnings("unchecked")
        @MainThread
        public void uncheckedBind(@NonNull final SettingsEntryConfig settingsEntryConfig) {
            bind((S) settingsEntryConfig);
        }
    }
    
    public abstract static class SingleBindSettingsViewHolder<V extends ViewBinding, S extends SettingsEntryConfig> extends SettingsViewHolder<V, S> {
        
        private boolean bound;
        
        SingleBindSettingsViewHolder(@NonNull ContextWrapper wrapper, @NonNull V viewBinding) {
            super(wrapper, viewBinding);
        }
        
        private void bindInternal(S config) {
            if (bound) {
                return;
            }
            bound = true;
            bind(config);
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public void uncheckedBind(@NonNull SettingsEntryConfig settingsEntryConfig) {
            bindInternal((S) settingsEntryConfig);
        }
    }
    
}

