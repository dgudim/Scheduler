package prototype.xd.scheduler.entities;

import static android.provider.CalendarContract.Events;
import static android.util.Log.ERROR;
import static android.util.Log.WARN;
import static prototype.xd.scheduler.utilities.DateManager.ONE_MINUTE_MS;
import static prototype.xd.scheduler.utilities.DateManager.daysToMs;
import static prototype.xd.scheduler.utilities.DateManager.msToDays;
import static prototype.xd.scheduler.utilities.DateManager.msUTCtoDaysLocal;
import static prototype.xd.scheduler.utilities.DateManager.msUTCtoMsLocal;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.QueryUtilities.getBoolean;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.calendarEventsColumns;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.generateSubKeysFromCalendarKey;
import static prototype.xd.scheduler.utilities.Utilities.rangesOverlap;
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
    
    protected List<String> subKeys;
    private String prefKey;
    long id;
    
    protected String title;
    public final int color;
    
    protected long startMsUTC;
    protected long endMsUTC;
    protected long durationMs;
    
    protected long startDayLocal;
    protected long endDayLocal;
    
    protected boolean isAllDay;
    
    protected RecurrenceSet rSet;
    
    TimeZone timeZone;
    
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
    
        computeEventVisibilityDays();
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
    
    public void computeEventVisibilityDays() {
        if (isAllDay) {
            // don't convert ms to local, all day events don't drift
            startDayLocal = msToDays(startMsUTC);
            endDayLocal = startDayLocal;
        } else {
            startDayLocal = msToDays(msUTCtoMsLocal(startMsUTC));
            endDayLocal = msToDays(msUTCtoMsLocal(endMsUTC));
        }
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
    
    protected boolean isRecurring() {
        return rSet != null;
    }
    
    @FunctionalInterface
    interface RecurrenceSetConsumer<T> {
        /**
         * A function to process each entry in a recurrence set
         *
         * @param instanceStartMsUTC ms since epoch of the instance
         * @return return value or null if should iterate forward
         */
        @Nullable
        T processInstance(long instanceStartMsUTC, long instanceEndMsUTC, long instanceStartDayLocal, long instanceEndDayLocal);
    }
    
    protected <T> T iterateRecurrenceSet(long firstDayUTC, RecurrenceSetConsumer<T> recurrenceSetConsumer, T defaultValue) {
        RecurrenceSetIterator it = rSet.iterator(timeZone, startMsUTC);
        it.fastForward(daysToMs(firstDayUTC - 2));
        long instanceStartMsUTC;
        long instanceEndMsUTC;
        T val;
        while (it.hasNext()) {
            instanceStartMsUTC = it.next();
            instanceEndMsUTC = instanceStartMsUTC + durationMs;
            val = recurrenceSetConsumer.processInstance(instanceStartMsUTC, instanceEndMsUTC,
                    isAllDay ? msToDays(instanceStartMsUTC) : msUTCtoDaysLocal(instanceStartMsUTC),
                    isAllDay ? msToDays(instanceEndMsUTC) : msUTCtoDaysLocal(instanceEndMsUTC));
            if (val != null) {
                return val;
            }
        }
        return defaultValue;
    }
    
    public boolean visibleOnRange(long firstDayUTC, long lastDayUTC) {
        if (rSet != null) {
            return iterateRecurrenceSet(firstDayUTC, (instanceStartMsUTC, instanceEndMsUTC, instanceStartDayLocal, instanceEndDayLocal) -> {
                // overshot
                if(instanceStartDayLocal > lastDayUTC) {
                    return false;
                }
                // if in range
                if (rangesOverlap(instanceStartDayLocal, instanceEndDayLocal, firstDayUTC, lastDayUTC)) {
                    return true;
                }
                return null;
            }, false);
        }
        return rangesOverlap(startDayLocal, endDayLocal, firstDayUTC, lastDayUTC);
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
        return NAME + " | " + prefKey;
    }
}
