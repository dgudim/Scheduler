package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.CALENDAR_ACCOUNT;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.utilities.SystemCalendarSettings;

public class CalendarAccountSettingsEntry extends SettingsEntry {
    
    private final Context context;
    private final ViewGroup root;
    private final SettingsFragment fragment;
    private final String accountName;
    private final String accountType;
    private final int calendarColor;
    
    public CalendarAccountSettingsEntry(final Context context, final ViewGroup root, final SettingsFragment fragment, final String accountName, final String accountType, final int calendarColor) {
        super(R.layout.account_entry);
        this.context = context;
        this.root = root;
        this.fragment = fragment;
        this.accountName = accountName;
        this.accountType = accountType;
        this.calendarColor = calendarColor;
        entryType = CALENDAR_ACCOUNT;
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        ((TextView) rootView.findViewById(R.id.calendar_name)).setText(accountName);
        ((TextView) rootView.findViewById(R.id.account_type)).setText(accountType);
        ((CheckBox) rootView.findViewById(R.id.check_box)).setButtonTintList(ColorStateList.valueOf(calendarColor));
        rootView.findViewById(R.id.edit_button).setOnClickListener(v -> new SystemCalendarSettings(context, fragment, LayoutInflater.from(context).inflate(R.layout.entry_settings, root, false), accountName));
        return super.InitInnerViews(rootView);
    }
}
