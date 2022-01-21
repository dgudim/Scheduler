package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.utilities.BitmapUtilities.createSolidColorCircle;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import prototype.xd.scheduler.R;

public class CalendarAccountSettingsEntry extends SettingsEntry {
    
    private final String calendarName;
    private final String accountType;
    private final int calendarColor;
    
    public CalendarAccountSettingsEntry(String calendarName, String accountType, int calendarColor) {
        super(R.layout.account_entry);
        this.calendarName = calendarName;
        this.accountType = accountType;
        this.calendarColor = calendarColor;
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        ((TextView) rootView.findViewById(R.id.calendar_name)).setText(calendarName);
        ((TextView) rootView.findViewById(R.id.account_type)).setText(accountType);
        ((ImageView) rootView.findViewById(R.id.calendar_color)).setImageBitmap(createSolidColorCircle(calendarColor));
        return super.InitInnerViews(rootView);
    }
}
