package prototype.xd.scheduler.entities.calendars;

import static android.provider.CalendarContract.Events;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.QueryUtilities.getBoolean;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarEventsColumns;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromKey;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.makeKey;
import static prototype.xd.scheduler.utilities.Utilities.RFC2445ToMilliseconds;

import android.database.Cursor;

import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;

import java.util.ArrayList;

public class SystemCalendarEvent {
    
    public final SystemCalendar associatedCalendar;
    
    public final long id;
    public final String title;
    public final int color;
    public final long start;
    public long end;
    public final boolean allDay;
    
    public RecurrenceRule rRule;
    public long duration;
    
    public final ArrayList<String> subKeys;
    
    SystemCalendarEvent(Cursor cursor, SystemCalendar associatedCalendar) {
        this.associatedCalendar = associatedCalendar;
        
        id = getLong(cursor, calendarEventsColumns, Events._ID);
        title = getString(cursor, calendarEventsColumns, Events.TITLE);
        color = getInt(cursor, calendarEventsColumns, Events.DISPLAY_COLOR);
        start = getLong(cursor, calendarEventsColumns, Events.DTSTART);
        end = getLong(cursor, calendarEventsColumns, Events.DTEND);
        allDay = getBoolean(cursor, calendarEventsColumns, Events.ALL_DAY);
        
        String rRule_str = getString(cursor, calendarEventsColumns, Events.RRULE);
        if (rRule_str != null) {
            try {
                rRule = new RecurrenceRule(rRule_str);
                duration = RFC2445ToMilliseconds(getString(cursor, calendarEventsColumns, Events.DURATION));
                DateTime until = rRule.getUntil();
                end = until == null ? Long.MAX_VALUE : until.getTimestamp();
            } catch (InvalidRecurrenceRuleException e) {
                logException(e);
            }
        }
        
        
        subKeys = generateSubKeysFromKey(makeKey(this));
    }
}
