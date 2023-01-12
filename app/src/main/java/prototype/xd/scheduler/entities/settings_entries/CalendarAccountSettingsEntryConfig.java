package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.CALENDAR_ACCOUNT;

import android.graphics.Color;

import androidx.annotation.NonNull;

import java.util.Locale;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.databinding.CalendarAccountSettingsEntryBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.GraphicsUtilities;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarAccountSettingsEntryConfig extends GenericCalendarSettingsEntryConfig {
    
    private final SystemCalendarSettings systemCalendarSettings;
    private final String accountName;
    private final String accountType;
    private final int calendarColor;
    
    private final SettingsListViewAdapter containerAdapter;
    
    public CalendarAccountSettingsEntryConfig(@NonNull final SystemCalendarSettings systemCalendarSettings,
                                              @NonNull final SystemCalendar calendar,
                                              @NonNull final SettingsListViewAdapter containerAdapter,
                                              boolean showSettings) {
        super(showSettings);
        this.systemCalendarSettings = systemCalendarSettings;
        this.accountName = calendar.account_name;
        this.accountType = calendar.account_type;
        this.calendarColor = calendar.color;
        this.containerAdapter = containerAdapter;
    }
    
    @Override
    public int getType() {
        return CALENDAR_ACCOUNT.ordinal();
    }
    
    static class CalendarAccountViewHolder extends SettingsEntryConfig.SettingsViewHolder<CalendarAccountSettingsEntryBinding, CalendarAccountSettingsEntryConfig> {
        
        CalendarAccountViewHolder(@NonNull ContextWrapper wrapper, @NonNull CalendarAccountSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(CalendarAccountSettingsEntryConfig config) {
            viewBinding.accountIcon.setImageResource(getIconFromAccountType(config.accountType));
            viewBinding.root.setBackgroundColor(GraphicsUtilities.mixTwoColors(config.calendarColor, Color.TRANSPARENT, 0.65));
            viewBinding.calendarName.setText(config.accountName);
            viewBinding.accountType.setText(config.accountType);
            viewBinding.settingsButton.setOnClickListener(v -> config.systemCalendarSettings.show(config.accountName, config.calendarColor));
            config.updateSettingsButtonVisibility(viewBinding.settingsButton);
            viewBinding.expandButton.setOnClickListener(v -> {
                config.containerAdapter.toggleCollapsed();
                updateCollapseIcon(config, true);
            });
            updateCollapseIcon(config, false);
        }
        
        private void updateCollapseIcon(CalendarAccountSettingsEntryConfig config, boolean animate) {
            int rotation = config.containerAdapter.isCollapsed() ? 0 : -90;
            if (animate) {
                viewBinding.expandButton
                        .animate()
                        .rotation(rotation)
                        .setDuration(wrapper.getInteger(R.integer.material_motion_duration_short_2))
                        .start();
            } else {
                viewBinding.expandButton.setRotation(rotation);
            }
        }
        
        private int getIconFromAccountType(String accountType) {
            String type = accountType.toLowerCase(Locale.ROOT);
            if (type.contains("exchange")) {
                return R.drawable.ic_microsoft_exchange_55;
            }
            if (type.contains("google")) {
                return R.drawable.ic_google_55;
            }
            if (type.contains("local")) {
                return R.drawable.ic_mobile_55;
            }
            return R.drawable.ic_account_circle_55;
        }
    }
}
