package prototype.xd.scheduler.entities.settingsEntries;

import static prototype.xd.scheduler.utilities.BitmapUtilities.createSolidColorCircle;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import prototype.xd.scheduler.R;

public class CalendarSettingsEntry extends SettingsEntry{
    
    private final String calendarName;
    private final int calendarColor;
    
    public CalendarSettingsEntry(String calendarName, int calendarColor) {
        super(R.layout.calendar_entry);
        this.calendarName = calendarName;
        this.calendarColor = calendarColor;
    }
    
    @Override
    protected View InitInnerViews(View rootView) {
        ((TextView) rootView.findViewById(R.id.calendar_name)).setText(calendarName);
        ((ImageView) rootView.findViewById(R.id.calendar_icon)).setImageBitmap(createSolidColorCircle(calendarColor));
        return super.InitInnerViews(rootView);
    }
}
