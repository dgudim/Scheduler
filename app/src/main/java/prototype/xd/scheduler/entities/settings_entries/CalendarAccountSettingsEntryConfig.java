package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.CALENDAR_ACCOUNT;

import androidx.annotation.NonNull;

import java.util.Locale;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.databinding.CalendarAccountSettingsEntryBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarAccountSettingsEntryConfig extends GenericCalendarSettingsEntryConfig {
    
    private final SystemCalendarSettings systemCalendarSettings;
    private final String accountName;
    private final String accountType;
    
    private final SettingsListViewAdapter containerAdapter;
    
    
    
    public CalendarAccountSettingsEntryConfig(@NonNull final SystemCalendarSettings systemCalendarSettings,
                                              @NonNull final SystemCalendar calendar,
                                              @NonNull final SettingsListViewAdapter containerAdapter,
                                              boolean showSettings) {
        super(showSettings);
        this.systemCalendarSettings = systemCalendarSettings;
        this.accountName = calendar.account_name;
        this.accountType = calendar.account_type;
        this.containerAdapter = containerAdapter;
    }
    
    @Override
    public int getType() {
        return CALENDAR_ACCOUNT.ordinal();
    }
    
    static class CalendarAccountViewHolder extends SettingsEntryConfig.SettingsViewHolder<CalendarAccountSettingsEntryBinding, CalendarAccountSettingsEntryConfig> {
        
        CalendarAccountViewHolder(CalendarAccountSettingsEntryBinding viewBinding) {
            super(viewBinding);
        }
        
        @Override
        void bind(CalendarAccountSettingsEntryConfig config) {
            viewBinding.accountIcon.setImageResource(getIconFromAccountType(config.accountType));
            viewBinding.calendarName.setText(config.accountName);
            viewBinding.accountType.setText(config.accountType);
            viewBinding.settingsButton.setOnClickListener(v -> config.systemCalendarSettings.show(config.accountName));
            config.updateSettingsButtonVisibility(viewBinding.settingsButton);
            viewBinding.expandButton.setOnClickListener(v -> {
                config.containerAdapter.toggleCollapsed();
                updateCollapseIcon(config, true);
            });
            updateCollapseIcon(config, false);
        }
        
        private void updateCollapseIcon(CalendarAccountSettingsEntryConfig config, boolean animate) {
            int rotation = config.containerAdapter.isCollapsed() ? 0 : -90;
            if(animate) {
                viewBinding.expandButton
                        .animate()
                        .rotation(rotation)
                        .setDuration(context.getResources().getInteger(R.integer.material_motion_duration_short_2))
                        .start();
            } else {
                viewBinding.expandButton.setRotation(rotation);
            }
        }
        
        private int getIconFromAccountType(String accountType) {
            String type = accountType.toLowerCase(Locale.ROOT);
            if (type.contains("exchange")) {
                return R.drawable.ic_microsoft_exchange;
            }
            if (type.contains("google")) {
                return R.drawable.ic_google;
            }
            if (type.contains("local")) {
                return R.drawable.ic_mobile;
            }
            return R.drawable.ic_account_circle;
        }
    }
}
