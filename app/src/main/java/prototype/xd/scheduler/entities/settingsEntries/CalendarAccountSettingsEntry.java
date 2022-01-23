package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.CALENDAR_ACCOUNT;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SettingsFragment;
import prototype.xd.scheduler.calendarUtilities.SystemCalendar;
import prototype.xd.scheduler.entities.Views.CheckBox;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.SystemCalendarSettings;

public class CalendarAccountSettingsEntry extends SettingsEntry {
    
    private final Context context;
    private final ViewGroup root;
    private final SettingsFragment fragment;
    private final String accountName;
    private final String accountType;
    private final int calendarColor;
    
    public CalendarAccountSettingsEntry(final Context context, final ViewGroup root, final SettingsFragment fragment, SystemCalendar calendar) {
        super(R.layout.account_entry);
        this.context = context;
        this.root = root;
        this.fragment = fragment;
        this.accountName = calendar.account_name;
        this.accountType = calendar.account_type;
        this.calendarColor = calendar.color;
        entryType = CALENDAR_ACCOUNT;
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        ((TextView) rootView.findViewById(R.id.calendar_name)).setText(accountName);
        ((TextView) rootView.findViewById(R.id.account_type)).setText(accountType);
        CheckBox checkBox = rootView.findViewById(R.id.check_box);
        checkBox.setButtonTintList(ColorStateList.valueOf(calendarColor));
        checkBox.setChecked(preferences.getBoolean(accountName + "_" + Keys.VISIBLE, Keys.CALENDAR_SETTINGS_DEFAULT_VISIBLE), false);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(accountName + "_" + Keys.VISIBLE, isChecked).apply();
        });
        rootView.findViewById(R.id.edit_button).setOnClickListener(v -> new SystemCalendarSettings(context, fragment, LayoutInflater.from(context).inflate(R.layout.entry_settings, root, false), accountName));
        return super.InitInnerViews(rootView);
    }
}
