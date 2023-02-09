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
    
    final long id;
    
    @Nullable
    protected String title;
    @ColorInt
    public final int color;
    
    protected final long refStartMsUTC;
    protected final long refEndMsUTC;
    
    protected final boolean isAllDay;
    
    protected final String rRuleStr;
    protected final String rDateStr;
    protected final String exRuleStr;
    protected final String exDateStr;
    
    protected final String timeZoneId;
    
    protected final List<Long> exceptions;
    
    public SystemCalendarEventData(@NonNull Cursor cursor) {
        exceptions = new ArrayList<>();
        
        color = getInt(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DISPLAY_COLOR);
        id = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events._ID);
        
        title = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.TITLE).trim();
        refStartMsUTC = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DTSTART);
        refEndMsUTC = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DTEND);
        isAllDay = getBoolean(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.ALL_DAY);
        
        timeZoneId = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.EVENT_TIMEZONE);
        
        exRuleStr = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.EXRULE);
        exDateStr = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.EXDATE);
        
        rRuleStr = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.RRULE);
        rDateStr = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.RDATE);
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
        SystemCalendarEventData that = (SystemCalendarEventData) o;
        return id == that.id &&
                color == that.color &&
                refStartMsUTC == that.refStartMsUTC &&
                refEndMsUTC == that.refEndMsUTC &&
                isAllDay == that.isAllDay &&
                Objects.equals(title, that.title) &&
                Objects.equals(rRuleStr, that.rRuleStr) &&
                Objects.equals(rDateStr, that.rDateStr) &&
                Objects.equals(exRuleStr, that.exRuleStr) &&
                Objects.equals(exDateStr, that.exDateStr) &&
                Objects.equals(timeZoneId, that.timeZoneId) &&
                Objects.equals(exceptions, that.exceptions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, title, color,
                refStartMsUTC, refEndMsUTC, isAllDay,
                rRuleStr, rDateStr, exRuleStr, exDateStr,
                timeZoneId, exceptions);
    }
    
    @NonNull
    @Override
    public String toString() {
        return NAME + ": " + (BuildConfig.DEBUG ? title : id) + " " + color;
    }
}
