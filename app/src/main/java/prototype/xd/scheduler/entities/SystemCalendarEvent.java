package prototype.xd.scheduler.entities;

import static android.provider.CalendarContract.Events;
import static prototype.xd.scheduler.utilities.DateManager.ONE_MINUTE_MS;
import static prototype.xd.scheduler.utilities.DateManager.daysToMs;
import static prototype.xd.scheduler.utilities.DateManager.msToDays;
import static prototype.xd.scheduler.utilities.DateManager.msUTCtoDaysLocal;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.QueryUtilities.getBoolean;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_EVENT_COLUMNS;
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
import java.util.Objects;
import java.util.TimeZone;

import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.utilities.Logger;

public class SystemCalendarEvent {
    
    public static final String NAME = SystemCalendarEvent.class.getSimpleName();
    
    protected @Nullable
    TodoEntry associatedEntry;
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
            color = getInt(cursor, CALENDAR_EVENT_COLUMNS, Events.DISPLAY_COLOR);
            return;
        }
        
        id = getLong(cursor, CALENDAR_EVENT_COLUMNS, Events._ID);
        
        title = getString(cursor, CALENDAR_EVENT_COLUMNS, Events.TITLE).trim();
        color = getInt(cursor, CALENDAR_EVENT_COLUMNS, Events.DISPLAY_COLOR);
        startMsUTC = getLong(cursor, CALENDAR_EVENT_COLUMNS, Events.DTSTART);
        endMsUTC = getLong(cursor, CALENDAR_EVENT_COLUMNS, Events.DTEND);
        isAllDay = getBoolean(cursor, CALENDAR_EVENT_COLUMNS, Events.ALL_DAY);
        
        prefKey = associatedCalendar.makePrefKey(color);
        subKeys = generateSubKeysFromCalendarKey(prefKey);
        
        String timeZoneId = getString(cursor, CALENDAR_EVENT_COLUMNS, Events.EVENT_TIMEZONE);
        
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
    
    private void loadRecurrenceRules(Cursor cursor) { // NOSONAR, not that complex
        
        String rRuleStr = getString(cursor, CALENDAR_EVENT_COLUMNS, Events.RRULE);
        String rDateStr = getString(cursor, CALENDAR_EVENT_COLUMNS, Events.RDATE);
        
        if (rRuleStr.length() > 0) {
            try {
                rSet = new RecurrenceSet();
                
                rSet.addInstances(new RecurrenceRuleAdapter(new RecurrenceRule(rRuleStr)));
                
                if (rDateStr.length() > 0) {
                    try {
                        DateTimeZonePair pair = checkRDates(rDateStr);
                        rSet.addInstances(new RecurrenceList(pair.date, pair.timeZone));
                    } catch (IllegalArgumentException e) {
                        Logger.error(NAME, "Error adding rDate: " + e.getMessage());
                    }
                }
                
                String exRuleStr = getString(cursor, CALENDAR_EVENT_COLUMNS, Events.EXRULE);
                String exDateStr = getString(cursor, CALENDAR_EVENT_COLUMNS, Events.EXDATE);
                
                if (exRuleStr.length() > 0) {
                    try {
                        rSet.addExceptions(new RecurrenceRuleAdapter(new RecurrenceRule(exRuleStr)));
                    } catch (IllegalArgumentException e) {
                        Logger.error(NAME, "Error adding exRule: " + e.getMessage());
                    }
                }
                
                if (exDateStr.length() > 0) {
                    try {
                        DateTimeZonePair pair = checkRDates(exDateStr);
                        rSet.addExceptions(new RecurrenceList(pair.date, pair.timeZone));
                    } catch (IllegalArgumentException e) {
                        Logger.error(NAME, "Error adding exDate: " + e.getMessage());
                    }
                }
                
                String durationStr = getString(cursor, CALENDAR_EVENT_COLUMNS, Events.DURATION);
                
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
            startDayLocal = msUTCtoDaysLocal(startMsUTC);
            endDayLocal = msUTCtoDaysLocal(endMsUTC);
        }
    }
    
    public boolean isAssociatedWithEntry() {
        return associatedEntry != null;
    }
    
    public void linkEntry(TodoEntry todoEntry) {
        if (associatedEntry != null) {
            Logger.warning(NAME, this + " already linked to " +
                    associatedEntry + " relinking to " + todoEntry);
        }
        associatedEntry = todoEntry;
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
            Logger.warning(NAME, "Not standard dates for " + this + ", " + datesToParse + ", probably contains timezone, attempting to parse");
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
                if (instanceStartDayLocal > lastDayUTC) {
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
    
    public void addExceptions(List<Long> exceptions) {
        if (rSet != null) {
            rSet.addExceptions(new RecurrenceList(exceptions.stream().mapToLong(Long::longValue).toArray()));
        } else {
            Logger.warning(NAME, "Couldn't add exceptions to " + this);
        }
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof SystemCalendarEvent) {
            // id is unique
            return id == ((SystemCalendarEvent) obj).id;
        }
        return false;
    }
    
    @NonNull
    @Override
    public String toString() {
        return NAME + ": " + (BuildConfig.DEBUG ? title : id) + " " + associatedCalendar.toString() + " " + color;
    }
}
