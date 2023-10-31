package prototype.xd.scheduler.entities;

import static prototype.xd.scheduler.utilities.QueryUtilities.getBoolean;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_EVENT_COLUMNS;

import android.database.Cursor;
import android.provider.CalendarContract;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.BuildConfig;

public class SystemCalendarEventData {
    
    public static final String NAME = SystemCalendarEvent.class.getSimpleName();
    
    protected final long id;
    
    @Nullable
    public final String title;
    public final String description;
    @ColorInt
    public final int color;
    
    protected final long refStartMsUTC;
    protected final long refEndMsUTC;
    
    protected final boolean allDay;
    
    protected final String rRuleStr;
    protected final String rDateStr;
    protected final String exRuleStr;
    protected final String exDateStr;
    protected final String durationStr;
    
    protected final String timeZoneId;
    
    protected final List<Long> exceptions;
    
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public SystemCalendarEventData(@NonNull Cursor cursor) {
        exceptions = new ArrayList<>();
        
        color = getInt(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DISPLAY_COLOR);
        id = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events._ID);
    
        title = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.TITLE);
        description = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DESCRIPTION);
        refStartMsUTC = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DTSTART);
        refEndMsUTC = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DTEND);
        allDay = getBoolean(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.ALL_DAY);
        
        timeZoneId = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.EVENT_TIMEZONE);
        
        exRuleStr = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.EXRULE);
        exDateStr = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.EXDATE);
        
        rRuleStr = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.RRULE);
        rDateStr = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.RDATE);
        
        durationStr = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DURATION);
    }
    
    public void addException(@NonNull Long exception) {
        exceptions.add(exception);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SystemCalendarEventData event = (SystemCalendarEventData) o;
        return id == event.id &&
                color == event.color &&
                refStartMsUTC == event.refStartMsUTC &&
                refEndMsUTC == event.refEndMsUTC &&
                allDay == event.allDay &&
                Objects.equals(title, event.title) &&
                Objects.equals(description, event.description) &&
                Objects.equals(rRuleStr, event.rRuleStr) &&
                Objects.equals(rDateStr, event.rDateStr) &&
                Objects.equals(exRuleStr, event.exRuleStr) &&
                Objects.equals(exDateStr, event.exDateStr) &&
                Objects.equals(durationStr, event.durationStr) &&
                Objects.equals(timeZoneId, event.timeZoneId) &&
                Objects.equals(exceptions, event.exceptions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, color,
                refStartMsUTC, refEndMsUTC, allDay,
                rRuleStr, rDateStr, exRuleStr, exDateStr, durationStr,
                timeZoneId, exceptions);
    }
    
    @NonNull
    @Override
    public String toString() {
        return NAME + ": " + (BuildConfig.DEBUG ? title : id) + " " + color;
    }
}
