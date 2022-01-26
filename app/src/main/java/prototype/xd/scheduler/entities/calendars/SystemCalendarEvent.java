package prototype.xd.scheduler.entities.calendars;

import static android.provider.CalendarContract.Events;
import static prototype.xd.scheduler.utilities.DateManager.timeZone_UTC;
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

import androidx.annotation.Nullable;

import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recurrenceset.RecurrenceList;
import org.dmfs.rfc5545.recurrenceset.RecurrenceRuleAdapter;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;

import java.util.ArrayList;
import java.util.Objects;

public class SystemCalendarEvent {
    
    public SystemCalendar associatedCalendar;
    
    public String title;
    public final int color;
    public long start;
    public long end;
    public boolean allDay;
    
    private String rRule_str; // for comparison
    private String rDate_str;
    private String exRule_str;
    private String exDate_str;
    
    public RecurrenceSet rSet;
    public long duration;
    
    public ArrayList<String> subKeys;
    
    SystemCalendarEvent(Cursor cursor, SystemCalendar associatedCalendar, boolean loadMinimal) {
        
        if (loadMinimal) {
            color = getInt(cursor, calendarEventsColumns, Events.DISPLAY_COLOR);
            return;
        }
        
        this.associatedCalendar = associatedCalendar;
        
        title = getString(cursor, calendarEventsColumns, Events.TITLE);
        color = getInt(cursor, calendarEventsColumns, Events.DISPLAY_COLOR);
        start = getLong(cursor, calendarEventsColumns, Events.DTSTART);
        end = getLong(cursor, calendarEventsColumns, Events.DTEND);
        allDay = getBoolean(cursor, calendarEventsColumns, Events.ALL_DAY);
        
        if (allDay) {
            end = start;
        }
        
        rRule_str = getString(cursor, calendarEventsColumns, Events.RRULE);
        rDate_str = getString(cursor, calendarEventsColumns, Events.RDATE);
        if (rRule_str != null || rDate_str != null) {
            try {
                rSet = new RecurrenceSet();
                
                if (rRule_str != null)
                    rSet.addInstances(new RecurrenceRuleAdapter(new RecurrenceRule(rRule_str)));
                
                if (rDate_str != null)
                    rSet.addInstances(new RecurrenceList(rDate_str, timeZone_UTC));
                
                exRule_str = getString(cursor, calendarEventsColumns, Events.EXRULE);
                exDate_str = getString(cursor, calendarEventsColumns, Events.EXDATE);
                
                if (exRule_str != null)
                    rSet.addExceptions(new RecurrenceRuleAdapter(new RecurrenceRule(exRule_str)));
                
                if (exDate_str != null)
                    rSet.addExceptions(new RecurrenceList(exDate_str, timeZone_UTC));
                
                duration = RFC2445ToMilliseconds(getString(cursor, calendarEventsColumns, Events.DURATION));
                
                end = rSet.isInfinite() ? Long.MAX_VALUE : rSet.getLastInstance(timeZone_UTC, start);
            } catch (InvalidRecurrenceRuleException e) {
                logException(e);
            }
        }
        
        subKeys = generateSubKeysFromKey(makeKey(this));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(title, color, start, end, allDay, rRule_str, rDate_str, exRule_str, exDate_str);
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof SystemCalendarEvent) {
            SystemCalendarEvent s_event = (SystemCalendarEvent) obj;
            return Objects.equals(title, s_event.title) &&
                    Objects.equals(color, s_event.color) &&
                    Objects.equals(start, s_event.start) &&
                    Objects.equals(end, s_event.end) &&
                    Objects.equals(allDay, s_event.allDay) &&
                    Objects.equals(rRule_str, s_event.rRule_str) &&
                    Objects.equals(rDate_str, s_event.rDate_str) &&
                    Objects.equals(exRule_str, s_event.exRule_str) &&
                    Objects.equals(exDate_str, s_event.exDate_str);
        }
        return super.equals(obj);
    }
}
