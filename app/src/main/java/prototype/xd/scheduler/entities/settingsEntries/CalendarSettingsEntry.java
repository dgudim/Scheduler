package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.entities.settingsEntries.SettingsEntryType.CALENDAR;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import prototype.xd.scheduler.R;

public class CalendarSettingsEntry extends SettingsEntry {
    
    private final String calendarName;
    private final int calendarColor;
    
    public CalendarSettingsEntry(String calendarName, int calendarColor) {
        super(R.layout.calendar_entry);
        this.calendarName = calendarName;
        this.calendarColor = calendarColor;
        entryType = CALENDAR;
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        ((TextView) rootView.findViewById(R.id.calendar_name)).setText(calendarName);
        ((CheckBox) rootView.findViewById(R.id.check_box)).setButtonTintList(ColorStateList.valueOf(calendarColor));
        return super.InitInnerViews(rootView);
    }
}
