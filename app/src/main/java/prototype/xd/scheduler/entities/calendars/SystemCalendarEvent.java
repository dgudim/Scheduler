package prototype.xd.scheduler.entities.calendars;

import static android.provider.CalendarContract.Events;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.timeZone_SYSTEM;
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
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.util.ArrayList;
import java.util.Objects;
import java.util.TimeZone;

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
    
    public TimeZone timeZone;
    
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
        
        timeZone = TimeZone.getTimeZone(getString(cursor, calendarEventsColumns, Events.EVENT_TIMEZONE));
        if (timeZone == null) {
            timeZone = timeZone_SYSTEM;
        }
        
        if (allDay) {
            end -= 60 * 1000;
            start += 60 * 1000;
        }
        
        duration = end - start;
        
        rRule_str = getString(cursor, calendarEventsColumns, Events.RRULE);
        rDate_str = getString(cursor, calendarEventsColumns, Events.RDATE);
        if (rRule_str != null || rDate_str != null) {
            try {
                rSet = new RecurrenceSet();
                
                if (rRule_str != null)
                    rSet.addInstances(new RecurrenceRuleAdapter(new RecurrenceRule(rRule_str)));
                
                if (rDate_str != null)
                    rSet.addInstances(new RecurrenceList(rDate_str, timeZone));
                
                exRule_str = getString(cursor, calendarEventsColumns, Events.EXRULE);
                exDate_str = getString(cursor, calendarEventsColumns, Events.EXDATE);
                
                if (exRule_str != null)
                    rSet.addExceptions(new RecurrenceRuleAdapter(new RecurrenceRule(exRule_str)));
                
                if (exDate_str != null)
                    rSet.addExceptions(new RecurrenceList(exDate_str, timeZone));
                
                duration = RFC2445ToMilliseconds(getString(cursor, calendarEventsColumns, Events.DURATION));
                
                end = rSet.isInfinite() ? Long.MAX_VALUE : rSet.getLastInstance(timeZone, start);
            } catch (InvalidRecurrenceRuleException e) {
                logException(e);
            }
        }
        
        if (allDay) {
            duration -= 60 * 1000;
        }
        
        subKeys = generateSubKeysFromKey(makeKey(this));
    }
    
    public boolean fallsInRange(long dayStart, long dayEnd) {
        if (rSet != null) {
            RecurrenceSetIterator it = rSet.iterator(timeZone, start);
            long instance = 0;
            while (it.hasNext() && daysFromEpoch(instance) <= dayEnd) {
                instance = daysFromEpoch(it.next());
                if (startOrEndInRange(start, start + duration, dayStart, dayEnd)) {
                    return true;
                }
            }
            return false;
        }
        return startOrEndInRange(start, end, dayStart, dayEnd);
    }
    
    private boolean startOrEndInRange(long start, long end, long dayStart, long dayEnd) {
        start = daysFromEpoch(start);
        end = daysFromEpoch(end);
        return (start >= dayStart && start <= dayEnd) || (end >= dayStart && end <= dayEnd);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(title, color, start, end, allDay, rRule_str, rDate_str, exRule_str, exDate_str, associatedCalendar);
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
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
                    Objects.equals(exDate_str, s_event.exDate_str) &&
                    Objects.equals(associatedCalendar, s_event.associatedCalendar);
        }
        return super.equals(obj);
    }
}
