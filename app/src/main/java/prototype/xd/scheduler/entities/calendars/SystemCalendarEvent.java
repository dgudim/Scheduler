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

import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recurrenceset.RecurrenceList;
import org.dmfs.rfc5545.recurrenceset.RecurrenceRuleAdapter;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;

import java.util.ArrayList;
import java.util.TimeZone;

public class SystemCalendarEvent {
    
    public final SystemCalendar associatedCalendar;
    
    public final long id;
    public final String title;
    public final int color;
    public final long start;
    public long end;
    public final boolean allDay;
    
    public RecurrenceSet rSet;
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
        
        if(allDay){
            end = start;
        }
        
        String rRule_str = getString(cursor, calendarEventsColumns, Events.RRULE);
        String rDate_str = getString(cursor, calendarEventsColumns, Events.RDATE);
        if (rRule_str != null || rDate_str != null) {
            try {
                rSet = new RecurrenceSet();
                
                if(rRule_str != null)
                    rSet.addInstances(new RecurrenceRuleAdapter(new RecurrenceRule(rRule_str)));
                
                if(rDate_str != null)
                    rSet.addInstances(new RecurrenceList(rDate_str, TimeZone.getTimeZone("UTC")));
                
                String exRule = getString(cursor, calendarEventsColumns, Events.EXRULE);
                String exDate = getString(cursor, calendarEventsColumns, Events.EXDATE);
    
                if(exRule != null)
                    rSet.addExceptions(new RecurrenceRuleAdapter(new RecurrenceRule(exRule)));
    
                if(exDate != null)
                    rSet.addExceptions(new RecurrenceList(exDate, TimeZone.getTimeZone("UTC")));
                
                duration = RFC2445ToMilliseconds(getString(cursor, calendarEventsColumns, Events.DURATION));
                
                end = rSet.isInfinite() ? Long.MAX_VALUE : rSet.getLastInstance(TimeZone.getTimeZone("UTC"), start);
            } catch (InvalidRecurrenceRuleException e) {
                logException(e);
            }
        }
        
        
        subKeys = generateSubKeysFromKey(makeKey(this));
    }
}
