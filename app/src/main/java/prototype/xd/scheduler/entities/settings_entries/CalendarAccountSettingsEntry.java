package prototype.xd.scheduler.entities.settings_entries;

import static prototype.xd.scheduler.entities.settings_entries.SettingsEntryType.CALENDAR_ACCOUNT;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;
import prototype.xd.scheduler.views.settings.SystemCalendarSettings;

public class CalendarAccountSettingsEntry extends SettingsEntry {
    
    private final SystemCalendarSettings systemCalendarSettings;
    private final String accountName;
    private final String accountType;
    
    public CalendarAccountSettingsEntry(final SystemCalendarSettings systemCalendarSettings, SystemCalendar calendar) {
        super(R.layout.account_entry);
        this.systemCalendarSettings = systemCalendarSettings;
        this.accountName = calendar.account_name;
        this.accountType = calendar.account_type;
        entryType = CALENDAR_ACCOUNT;
    }
    
    private int getIconFromAccountType() {
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
    
    @Override
    protected View initInnerViews(View convertView, ViewGroup viewGroup) {
        ((ImageView) convertView.findViewById(R.id.account_icon)).setImageResource(getIconFromAccountType());
        ((TextView) convertView.findViewById(R.id.calendar_name)).setText(accountName);
        ((TextView) convertView.findViewById(R.id.account_type)).setText(accountType);
        convertView.findViewById(R.id.edit_button).setOnClickListener(v -> systemCalendarSettings.show(accountName));
        return super.initInnerViews(convertView, viewGroup);
    }
}
