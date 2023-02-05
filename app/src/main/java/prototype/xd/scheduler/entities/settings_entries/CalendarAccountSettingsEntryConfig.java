package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.CALENDAR_ACCOUNT;
import static prototype.xd.scheduler.utilities.GraphicsUtilities.dimColorToBg;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.databinding.CalendarAccountSettingsEntryBinding;
import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarAccountSettingsEntryConfig extends GenericCalendarSettingsEntryConfig {
    
    @NonNull
    private final SystemCalendarSettings systemCalendarSettings;
    @NonNull
    private final String accountName;
    @NonNull
    private final List<String> subKeys;
    @NonNull
    private final String accountType;
    @ColorInt
    private final int calendarColor;
    
    @NonNull
    private final SettingsListViewAdapter containerAdapter;
    
    public CalendarAccountSettingsEntryConfig(@NonNull final SystemCalendarSettings systemCalendarSettings,
                                              @NonNull final SystemCalendar calendar,
                                              @NonNull final SettingsListViewAdapter containerAdapter,
                                              boolean showSettings) {
        super(showSettings);
        this.systemCalendarSettings = systemCalendarSettings;
        accountName = calendar.accountName;
        subKeys = Collections.singletonList(accountName);
        accountType = calendar.accountType;
        calendarColor = calendar.color;
        this.containerAdapter = containerAdapter;
    }
    
    @Override
    public int getRecyclerViewType() {
        return CALENDAR_ACCOUNT.ordinal();
    }
    
    static class CalendarAccountViewHolder extends SettingsEntryConfig.SettingsViewHolder<CalendarAccountSettingsEntryBinding, CalendarAccountSettingsEntryConfig> {
        
        CalendarAccountViewHolder(@NonNull ContextWrapper wrapper, @NonNull CalendarAccountSettingsEntryBinding viewBinding) {
            super(wrapper, viewBinding);
        }
        
        @Override
        void bind(@NonNull CalendarAccountSettingsEntryConfig config) {
            viewBinding.accountIcon.setImageResource(getIconFromAccountType(config.accountType));
            viewBinding.root.setBackgroundColor(dimColorToBg(config.calendarColor, viewBinding.root.getContext(), Static.CALENDAR_SETTINGS_DIM_FACTOR));
            viewBinding.calendarName.setText(config.accountName);
            viewBinding.accountType.setText(config.accountType);
            viewBinding.settingsButton.setOnClickListener(v ->
                    config.systemCalendarSettings.show(config.accountName, config.subKeys, config.calendarColor));
            config.updateSettingsButtonVisibility(viewBinding.settingsButton);
            View.OnClickListener expandListener = v -> {
                config.containerAdapter.toggleCollapsed();
                updateCollapseIcon(config, true);
            };
            viewBinding.expandButton.setOnClickListener(expandListener);
            viewBinding.root.setOnClickListener(expandListener);
            updateCollapseIcon(config, false);
        }
        
        private void updateCollapseIcon(@NonNull CalendarAccountSettingsEntryConfig config, boolean animate) {
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
        
        private static int getIconFromAccountType(@NonNull String accountType) {
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
