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

import org.dmfs.rfc5545.recurrenceset.RecurrenceSet;

import java.util.List;
import java.util.TimeZone;

public class SystemCalendarEventData {
    
    @NonNull
    private final String prefKey;
    final long id;
    
    @Nullable
    protected String title;
    @ColorInt
    public final int color;
    
    protected long startMsUTC;
    protected long endMsUTC;
    protected long durationMs;
    
    protected final boolean isAllDay;
    
    
    private final TimeZone timeZone;
    
    
    public SystemCalendarEventData(@NonNull Cursor cursor) {
        color = getInt(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DISPLAY_COLOR);
        id = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events._ID);
        
        prefKey = associatedCalendar.makeEventPrefKey(color);
        
        title = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.TITLE).trim();
        startMsUTC = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DTSTART);
        endMsUTC = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.DTEND);
        isAllDay = getBoolean(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.ALL_DAY);
        
        String timeZoneId = getString(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.EVENT_TIMEZONE);
        
        timeZone = TimeZone.getTimeZone(timeZoneId.isEmpty() ? associatedCalendar.timeZoneId : timeZoneId);
    }
    
    
}
