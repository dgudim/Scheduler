package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.CALENDAR_ACCOUNT;

import java.util.Locale;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.CalendarAccountSettingsEntryBinding;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarAccountSettingsEntryConfig extends SettingsEntryConfig {
    
    private final SystemCalendarSettings systemCalendarSettings;
    private final String accountName;
    private final String accountType;
    
    public CalendarAccountSettingsEntryConfig(final SystemCalendarSettings systemCalendarSettings, SystemCalendar calendar) {
        this.systemCalendarSettings = systemCalendarSettings;
        this.accountName = calendar.account_name;
        this.accountType = calendar.account_type;
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
            viewBinding.editButton.setOnClickListener(v -> config.systemCalendarSettings.show(config.accountName));
        }
        
        private int getIconFromAccountType(String accountType) {
            String type = accountType.toLowerCase(Locale.ROOT);
            if(type.contains("exchange")) {
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
