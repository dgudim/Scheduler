package prototype.xd.scheduler.entities;

import static android.provider.CalendarContract.Events;
import static android.util.Log.ERROR;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.DateManager.ONE_MINUTE_MS;
import static prototype.xd.scheduler.utilities.DateManager.daysUTCFromMsUTC;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.QueryUtilities.getBoolean;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarEventsColumns;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromCalendarKey;
import static prototype.xd.scheduler.utilities.Utilities.rfc2445ToMilliseconds;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recurrenceset.RecurrenceList;
import org.dmfs.rfc5545.recurrenceset.RecurrenceRuleAdapter;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.util.List;
import java.util.TimeZone;

public class SystemCalendarEvent {
    
    private static final String NAME = "System calendar event";
    
    protected @Nullable
    TodoListEntry associatedEntry;
    protected final SystemCalendar associatedCalendar;
    
    public List<String> subKeys;
    private String prefKey;
    long id;
    
    protected String title;
    public final int color;
    
    protected long startMsUTC;
    protected long endMsUTC;
    protected long durationMs;
    
    protected long startDayUTC;
    protected long endDayUTC;
    protected long durationDays;
    
    protected boolean isAllDay;
    
    protected RecurrenceSet rSet;
    
    protected TimeZone timeZone;
    
    SystemCalendarEvent(Cursor cursor, SystemCalendar associatedCalendar, boolean loadMinimal) {
        
        this.associatedCalendar = associatedCalendar;
        
        if (loadMinimal) {
            color = getInt(cursor, calendarEventsColumns, Events.DISPLAY_COLOR);
            return;
        }
        
        id = getLong(cursor, calendarEventsColumns, Events._ID);
        
        title = getString(cursor, calendarEventsColumns, Events.TITLE).trim();
        color = getInt(cursor, calendarEventsColumns, Events.DISPLAY_COLOR);
        startMsUTC = getLong(cursor, calendarEventsColumns, Events.DTSTART);
        endMsUTC = getLong(cursor, calendarEventsColumns, Events.DTEND);
        isAllDay = getBoolean(cursor, calendarEventsColumns, Events.ALL_DAY);
        
        prefKey = associatedCalendar.makeKey(color);
        subKeys = generateSubKeysFromCalendarKey(prefKey);
        
        String timeZoneId = getString(cursor, calendarEventsColumns, Events.EVENT_TIMEZONE);
        
        timeZone = TimeZone.getTimeZone(timeZoneId.isEmpty() ? associatedCalendar.timeZoneId : timeZoneId);
        
        durationMs = endMsUTC - startMsUTC;
        
        loadRecurrenceRules(cursor);
        
        // shorten the event a little because it's bounds are on midnight and that breaks stuff
        if (isAllDay) {
            endMsUTC -= ONE_MINUTE_MS;
            startMsUTC += ONE_MINUTE_MS;
            durationMs -= 2 * ONE_MINUTE_MS;
        }
    }
    
    private void loadRecurrenceRules(Cursor cursor) {
        
        String rRuleStr = getString(cursor, calendarEventsColumns, Events.RRULE);
        String rDateStr = getString(cursor, calendarEventsColumns, Events.RDATE);
        
        if (rRuleStr.length() > 0) {
            try {
                rSet = new RecurrenceSet();
        
                rSet.addInstances(new RecurrenceRuleAdapter(new RecurrenceRule(rRuleStr)));
        
                if (rDateStr.length() > 0) {
                    try {
                        DateTimeZonePair pair = checkRDates(rDateStr);
                        rSet.addInstances(new RecurrenceList(pair.date, pair.timeZone));
                    } catch (IllegalArgumentException e) {
                        log(ERROR, NAME, "Error adding rDate: " + e.getMessage());
                    }
                }
        
                String exRuleStr = getString(cursor, calendarEventsColumns, Events.EXRULE);
                String exDateStr = getString(cursor, calendarEventsColumns, Events.EXDATE);
        
                if (exRuleStr.length() > 0) {
                    try {
                        rSet.addExceptions(new RecurrenceRuleAdapter(new RecurrenceRule(exRuleStr)));
                    } catch (IllegalArgumentException e) {
                        log(ERROR, NAME, "Error adding exRule: " + e.getMessage());
                    }
                }
        
                if (exDateStr.length() > 0) {
                    try {
                        DateTimeZonePair pair = checkRDates(exDateStr);
                        rSet.addExceptions(new RecurrenceList(pair.date, pair.timeZone));
                    } catch (IllegalArgumentException e) {
                        log(ERROR, NAME, "Error adding exDate: " + e.getMessage());
                    }
                }
        
                String durationStr = getString(cursor, calendarEventsColumns, Events.DURATION);
        
                if (durationStr.length() > 0) {
                    durationMs = rfc2445ToMilliseconds(durationStr);
                }
        
                endMsUTC = rSet.isInfinite() ? Long.MAX_VALUE / 2 : rSet.getLastInstance(timeZone, startMsUTC);
            } catch (Exception e) {
                logException(NAME, e);
            }
        }
    }
    
    public void computeDurationInDays() {
        startDayUTC = daysUTCFromMsUTC(startMsUTC);
        endDayUTC = daysUTCFromMsUTC(endMsUTC);
        durationDays = daysUTCFromMsUTC(startMsUTC + durationMs) - startDayUTC;
    }
    
    public boolean isAssociatedWithEntry() {
        return associatedEntry != null;
    }
    
    public void linkEntry(TodoListEntry todoListEntry) {
        if (associatedEntry != null) {
            log(WARN, NAME, "Calendar event " + title + " already linked to " +
                    associatedEntry.getId() + " relinking to " + todoListEntry.getId());
        }
        associatedEntry = todoListEntry;
    }
    
    public void unlinkEntry() {
        associatedEntry = null;
    }
    
    
    // ------------------------------ METHODS FOR WORKING WITH ENTRY PARAMETERS START
    
    /**
     * Invalidate one parameters of the connected entry
     *
     * @param parameterKey parameter key to invalidate
     */
    protected void invalidateParameter(String parameterKey) {
        if (associatedEntry != null) {
            associatedEntry.invalidateParameter(parameterKey, true);
        }
    }
    
    /**
     * Invalidate all parameters of the connected entry, useful for settings reset for example
     */
    protected void invalidateAllParameters() {
        if (associatedEntry != null) {
            associatedEntry.invalidateAllParameters(true);
        }
    }
    
    /**
     * Invalidate one parameter of all connected entries
     *
     * @param parameterKey parameter key to invalidate
     */
    public void invalidateParameterOfConnectedEntries(String parameterKey) {
        associatedCalendar.invalidateParameterOnEvents(parameterKey, color);
    }
    
    /**
     * Invalidate all parameters of all connected entries
     */
    public void invalidateAllParametersOfConnectedEntries() {
        associatedCalendar.invalidateAllParametersOnEvents(color);
    }
    // ------------------------------ METHODS FOR WORKING WITH ENTRY PARAMETERS END
    
    
    private DateTimeZonePair checkRDates(String datesToParse) {
        TimeZone newTimeZone = timeZone;
        if (datesToParse.contains(";")) {
            log(WARN, NAME, "Not standard dates for " + title + ", " + datesToParse + ", probably contains timezone, attempting to parse");
            String[] split = datesToParse.split(";");
            newTimeZone = TimeZone.getTimeZone(split[0]);
            datesToParse = split[1];
        }
        return new DateTimeZonePair(datesToParse, newTimeZone);
    }
    
    public String getKey() {
        return prefKey;
    }
    
    private static class DateTimeZonePair {
        
        final String date;
        final TimeZone timeZone;
        
        DateTimeZonePair(String date, TimeZone timeZone) {
            this.date = date;
            this.timeZone = timeZone;
        }
    }
    
    public boolean fallsInRange(long dayStart, long dayEnd) {
        if (rSet != null) {
            RecurrenceSetIterator it = rSet.iterator(timeZone, startMsUTC);
            long instanceMsUTC = 0;
            while (it.hasNext() && daysUTCFromMsUTC(instanceMsUTC) <= dayEnd) {
                instanceMsUTC = it.next();
                if (instanceVisible(instanceMsUTC, instanceMsUTC + durationMs, dayStart, dayEnd)) {
                    return true;
                }
            }
            return false;
        }
        return instanceVisible(startMsUTC, endMsUTC, dayStart, dayEnd);
    }
    
    private boolean instanceVisible(long startMsUTC, long endMsUTC, long dayStart, long dayEnd) {
        return daysUTCFromMsUTC(startMsUTC) <= dayEnd &&
                daysUTCFromMsUTC(endMsUTC) >= dayStart;
    }
    
    public void addExceptions(Long[] exceptions) {
        if (rSet != null) {
            // whyyyyyyy, but ok
            long[] exceptionsPrimitiveArray = new long[exceptions.length];
            for (int i = 0; i < exceptions.length; i++) {
                exceptionsPrimitiveArray[i] = exceptions[i];
            }
            rSet.addExceptions(new RecurrenceList(exceptionsPrimitiveArray));
        } else {
            log(WARN, NAME, "Couldn't add exceptions to " + title);
        }
    }
    
    @NonNull
    @Override
    public String toString() {
        return "SystemCalendarEvent " + prefKey;
    }
}
