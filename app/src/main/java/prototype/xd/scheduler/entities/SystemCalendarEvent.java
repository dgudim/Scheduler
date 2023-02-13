package prototype.xd.scheduler.entities;

import static prototype.xd.scheduler.utilities.DateManager.ONE_MINUTE_MS;
import static prototype.xd.scheduler.utilities.DateManager.daysToMs;
import static prototype.xd.scheduler.utilities.DateManager.msToDays;
import static prototype.xd.scheduler.utilities.DateManager.msUTCtoDaysLocal;
import static prototype.xd.scheduler.utilities.Logger.logException;
import static prototype.xd.scheduler.utilities.Utilities.doRangesOverlap;
import static prototype.xd.scheduler.utilities.Utilities.rfc2445ToMilliseconds;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.primitives.Longs;

import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recurrenceset.RecurrenceList;
import org.dmfs.rfc5545.recurrenceset.RecurrenceRuleAdapter;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;

import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import prototype.xd.scheduler.utilities.Logger;

public class SystemCalendarEvent {
    
    public static final String NAME = SystemCalendarEvent.class.getSimpleName();
    
    @Nullable
    protected TodoEntry associatedEntry;
    @NonNull
    protected final SystemCalendar associatedCalendar;
    
    @NonNull
    public final List<String> subKeys;
    @NonNull
    public final String prefKey;
    
    protected long startMsUTC;
    protected long endMsUTC;
    protected long durationMs;
    
    protected long startDayLocal;
    protected long endDayLocal;
    
    @NonNull
    public final SystemCalendarEventData data;
    
    @Nullable
    protected RecurrenceSet rSet;
    private final TimeZone timeZone;
    
    SystemCalendarEvent(@NonNull SystemCalendarEventData data,
                        @NonNull SystemCalendar associatedCalendar) {
        
        this.associatedCalendar = associatedCalendar;
        this.data = data;
        
        prefKey = associatedCalendar.makeEventPrefKey(data.color);
        subKeys = associatedCalendar.makeEventSubKeys(prefKey);
        
        timeZone = TimeZone.getTimeZone(data.timeZoneId.isEmpty() ? associatedCalendar.data.timeZoneId : data.timeZoneId);
        
        startMsUTC = data.refStartMsUTC;
        endMsUTC = data.refEndMsUTC;
        durationMs = endMsUTC - startMsUTC;
        
        loadRecurrenceRules();
        
        // shorten the event a little because it's bounds are on midnight and that breaks stuff
        if (data.allDay) {
            endMsUTC -= ONE_MINUTE_MS;
            startMsUTC += ONE_MINUTE_MS;
            durationMs -= 2 * ONE_MINUTE_MS;
        }
        
        computeEventVisibilityDays();
    }
    
    public boolean isAllDay() {
        return data.allDay;
    }
    
    public void computeEventVisibilityDays() {
        if (data.allDay) {
            // don't convert ms to local, all day events don't drift
            startDayLocal = msToDays(startMsUTC);
            endDayLocal = startDayLocal;
        } else {
            startDayLocal = msUTCtoDaysLocal(startMsUTC);
            endDayLocal = msUTCtoDaysLocal(endMsUTC);
        }
    }
    
    private void loadRecurrenceRules() { // NOSONAR, not that complex
        
        if (!data.rRuleStr.isEmpty()) {
            try {
                rSet = new RecurrenceSet();
                
                rSet.addInstances(new RecurrenceRuleAdapter(new RecurrenceRule(data.rRuleStr)));
                
                if (!data.rDateStr.isEmpty()) {
                    try {
                        DateTimeZonePair pair = getRecurrenceDates(data.rDateStr);
                        rSet.addInstances(new RecurrenceList(pair.dateList, pair.timeZone));
                    } catch (IllegalArgumentException e) {
                        Logger.error(NAME, "Error adding rDate: " + e.getMessage());
                    }
                }
                
                if (!data.exRuleStr.isEmpty()) {
                    try {
                        rSet.addExceptions(new RecurrenceRuleAdapter(new RecurrenceRule(data.exRuleStr)));
                    } catch (IllegalArgumentException e) {
                        Logger.error(NAME, "Error adding exRule: " + e.getMessage());
                    }
                }
                
                if (!data.exDateStr.isEmpty()) {
                    try {
                        DateTimeZonePair pair = getRecurrenceDates(data.exDateStr);
                        rSet.addExceptions(new RecurrenceList(pair.dateList, pair.timeZone));
                    } catch (IllegalArgumentException e) {
                        Logger.error(NAME, "Error adding exDate: " + e.getMessage());
                    }
                }
                
                if (!data.durationStr.isEmpty()) {
                    durationMs = rfc2445ToMilliseconds(data.durationStr);
                }
                
                endMsUTC = rSet.isInfinite() ? Long.MAX_VALUE / 2 : rSet.getLastInstance(timeZone, startMsUTC);
                
                if (!data.exceptions.isEmpty()) {
                    rSet.addExceptions(new RecurrenceList(Longs.toArray(data.exceptions)));
                }
            } catch (Exception e) {
                logException(NAME, e);
            }
        }
    }
    
    @NonNull
    private DateTimeZonePair getRecurrenceDates(@NonNull String datesToParse) {
        TimeZone newTimeZone = timeZone;
        if (datesToParse.contains(";")) {
            Logger.warning(NAME, "Not standard dates for " + this + ", " + datesToParse + ", probably contains timezone, attempting to parse");
            String[] split = datesToParse.split(";");
            newTimeZone = TimeZone.getTimeZone(split[0]);
            datesToParse = split[1];
        }
        return new DateTimeZonePair(datesToParse, newTimeZone);
    }
    
    public boolean isAssociatedWithEntry() {
        return associatedEntry != null;
    }
    
    public void linkEntry(@NonNull TodoEntry todoEntry) {
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
    protected void invalidateParameter(@NonNull String parameterKey) {
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
    public void invalidateParameterOfConnectedEntries(@NonNull String parameterKey) {
        associatedCalendar.invalidateParameterOnEvents(parameterKey, data.color);
    }
    
    /**
     * Invalidate all parameters of all connected entries
     */
    public void invalidateAllParametersOfConnectedEntries() {
        associatedCalendar.invalidateParameterOnEvents(null, data.color);
    }
    // ------------------------------ METHODS FOR WORKING WITH ENTRY PARAMETERS END
    
    
    private static class DateTimeZonePair {
        
        @NonNull
        final String dateList;
        @NonNull
        final TimeZone timeZone;
        
        DateTimeZonePair(@NonNull String dateList, @NonNull TimeZone timeZone) {
            this.dateList = dateList;
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
    
    protected <T> T iterateRecurrenceSet(long firstDayUTC, @NonNull RecurrenceSetConsumer<T> recurrenceSetConsumer, @Nullable T defaultValue) {
        RecurrenceSetIterator it = Objects.requireNonNull(rSet).iterator(timeZone, startMsUTC);
        it.fastForward(daysToMs(firstDayUTC - 2));
        long instanceStartMsUTC;
        long instanceEndMsUTC;
        T val;
        while (it.hasNext()) {
            instanceStartMsUTC = it.next();
            instanceEndMsUTC = instanceStartMsUTC + durationMs;
            val = recurrenceSetConsumer.processInstance(instanceStartMsUTC, instanceEndMsUTC,
                    data.allDay ? msToDays(instanceStartMsUTC) : msUTCtoDaysLocal(instanceStartMsUTC),
                    data.allDay ? msToDays(instanceEndMsUTC) : msUTCtoDaysLocal(instanceEndMsUTC));
            if (val != null) {
                return val;
            }
        }
        return defaultValue;
    }
    
    public boolean isVisibleOnRange(long firstDayUTC, long lastDayUTC) {
        if (isRecurring()) {
            return iterateRecurrenceSet(firstDayUTC, (instanceStartMsUTC, instanceEndMsUTC, instanceStartDayLocal, instanceEndDayLocal) -> {
                // overshot
                if (instanceStartDayLocal > lastDayUTC) {
                    return Boolean.FALSE;
                }
                // if in range
                if (doRangesOverlap(instanceStartDayLocal, instanceEndDayLocal, firstDayUTC, lastDayUTC)) {
                    return Boolean.TRUE;
                }
                return null;
            }, Boolean.FALSE);
        }
        return doRangesOverlap(startDayLocal, endDayLocal, firstDayUTC, lastDayUTC);
    }
    
    @Override
    public int hashCode() {
        return data.hashCode();
    }
    
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return data.equals(((SystemCalendarEvent) o).data);
    }
    
    @NonNull
    @Override
    public String toString() {
        return data + " from " + associatedCalendar;
    }
}
