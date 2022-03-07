package prototype.xd.scheduler.entities.calendars;

import static android.provider.CalendarContract.Events;
import static android.util.Log.ERROR;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.Logger.log;
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
        
        title = getString(cursor, calendarEventsColumns, Events.TITLE).trim();
        color = getInt(cursor, calendarEventsColumns, Events.DISPLAY_COLOR);
        start = getLong(cursor, calendarEventsColumns, Events.DTSTART);
        end = getLong(cursor, calendarEventsColumns, Events.DTEND);
        allDay = getBoolean(cursor, calendarEventsColumns, Events.ALL_DAY);
        
        String timeZoneId = getString(cursor, calendarEventsColumns, Events.EVENT_TIMEZONE);
        
        timeZone = TimeZone.getTimeZone(timeZoneId == null ? associatedCalendar.timeZone.getID() : timeZoneId);
        
        duration = end - start;
        
        rRule_str = nullWrapper(getString(cursor, calendarEventsColumns, Events.RRULE));
        rDate_str = nullWrapper(getString(cursor, calendarEventsColumns, Events.RDATE));
        if (rRule_str.length() > 0) {
            try {
                rSet = new RecurrenceSet();
                
                rSet.addInstances(new RecurrenceRuleAdapter(new RecurrenceRule(rRule_str)));
                
                if (rDate_str.length() > 0) {
                    try {
                        DateTimeZonePair pair = checkRDates(rDate_str);
                        rSet.addInstances(new RecurrenceList(pair.date, pair.timeZone));
                    } catch (IllegalArgumentException e) {
                        log(ERROR, "SystemCalendarEvent", "Error adding rDate: " + e.getMessage());
                    }
                }
                
                exRule_str = nullWrapper(getString(cursor, calendarEventsColumns, Events.EXRULE));
                exDate_str = nullWrapper(getString(cursor, calendarEventsColumns, Events.EXDATE));
                
                if (exRule_str.length() > 0) {
                    try {
                        rSet.addExceptions(new RecurrenceRuleAdapter(new RecurrenceRule(exRule_str)));
                    } catch (IllegalArgumentException e) {
                        log(ERROR, "SystemCalendarEvent", "Error adding exRule: " + e.getMessage());
                    }
                }
                
                if (exDate_str.length() > 0) {
                    try {
                        DateTimeZonePair pair = checkRDates(exDate_str);
                        rSet.addExceptions(new RecurrenceList(pair.date, pair.timeZone));
                    } catch (IllegalArgumentException e) {
                        log(ERROR, "SystemCalendarEvent", "Error adding exDate: " + e.getMessage());
                    }
                }
                
                String duration_str = nullWrapper(getString(cursor, calendarEventsColumns, Events.DURATION));
                
                if (duration_str.length() > 0) {
                    duration = RFC2445ToMilliseconds(duration_str);
                }
                
                end = rSet.isInfinite() ? Long.MAX_VALUE / 2 : rSet.getLastInstance(timeZone, start);
            } catch (Exception e) {
                logException("SystemCalendarEvent", e);
            }
        }
        
        if (allDay) {
            end -= 60 * 1000;
            start += 60 * 1000;
            duration -= 2 * 60 * 1000;
        }
        
        subKeys = generateSubKeysFromKey(makeKey(this));
    }
    
    private DateTimeZonePair checkRDates(String datesToParse){
        TimeZone newTimeZone = timeZone;
        if(datesToParse.contains(";")){
            log(WARN, "SystemCalendarEvent", "Not standard dates for " + title + ", " + datesToParse + ", probably contains timezone, attempting to parse");
            String[] split = datesToParse.split(";");
            newTimeZone = TimeZone.getTimeZone(split[0]);
            datesToParse = split[1];
        }
        return new DateTimeZonePair(datesToParse, newTimeZone);
    }
    
    private static class DateTimeZonePair{
    
        String date;
        TimeZone timeZone;
        
        DateTimeZonePair(String date, TimeZone timeZone){
            this.date = date;
            this.timeZone = timeZone;
        }
    }
    
    private String nullWrapper(String str) {
        return str == null ? "" : str.trim();
    }
    
    public boolean fallsInRange(long dayStart, long dayEnd) {
        if (rSet != null) {
            RecurrenceSetIterator it = rSet.iterator(timeZone, start);
            long instance = 0;
            while (it.hasNext() && daysFromEpoch(instance, timeZone) <= dayEnd) {
                instance = it.next();
                if (rangesOverlap(instance, instance + duration, dayStart, dayEnd)) {
                    return true;
                }
            }
            return false;
        }
        return rangesOverlap(start, end, dayStart, dayEnd);
    }
    
    private boolean rangesOverlap(long start, long end, long dayStart, long dayEnd) {
        return daysFromEpoch(start, timeZone) <= dayEnd && daysFromEpoch(end, timeZone) >= dayStart;
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
