package prototype.xd.scheduler.entities;

import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_COLUMNS;

import android.database.Cursor;
import android.provider.CalendarContract;
import android.util.ArrayMap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.Map;
import java.util.Objects;

import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.utilities.Logger;

public class SystemCalendarData {
    
    public static final String NAME = SystemCalendar.class.getSimpleName();
    
    @NonNull
    public final String accountType;
    @NonNull
    public final String accountName;
    
    public final long id;
    public final int accessLevel;
    
    @NonNull
    public final String timeZoneId;
    @NonNull
    public final String displayName;
    @ColorInt
    public final int color;
    
    @NonNull
    public final Map<Long, SystemCalendarEventData> systemCalendarEventsData;
    
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public SystemCalendarData(@NonNull Cursor cursor) {
        
        accountType = getString(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.ACCOUNT_TYPE);
        accountName = getString(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.ACCOUNT_NAME);
        displayName = getString(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars._ID);
        accessLevel = getInt(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.CALENDAR_COLOR);
        
        String calTimeZoneId = getString(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.CALENDAR_TIME_ZONE);
        
        if (calTimeZoneId.isEmpty()) {
            Logger.warning(NAME, this + " has no timezone, defaulting to UTC");
            timeZoneId = "UTC";
        } else {
            timeZoneId = calTimeZoneId;
        }
        
        systemCalendarEventsData = new ArrayMap<>();
    }
    
    public void addEventData(@NonNull SystemCalendarEventData data) {
        systemCalendarEventsData.put(data.id, data);
    }
    
    public void addExceptionToEvent(long eventId, long exception) {
        SystemCalendarEventData eventData = systemCalendarEventsData.get(eventId);
        if (eventData == null) {
            Logger.warning(NAME, "Id " + eventId + " not found in " + this);
            return;
        }
        eventData.addException(exception);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SystemCalendarData other = (SystemCalendarData) o;
        return id == other.id &&
                accessLevel == other.accessLevel &&
                color == other.color &&
                accountType.equals(other.accountType) &&
                accountName.equals(other.accountName) &&
                displayName.equals(other.displayName) &&
                timeZoneId.equals(other.timeZoneId) &&
                systemCalendarEventsData.equals(other.systemCalendarEventsData);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(accountType, accountName, displayName, id, accessLevel, timeZoneId, color, systemCalendarEventsData);
    }
    
    @NonNull
    @Override
    public String toString() {
        return NAME + ": " + (BuildConfig.DEBUG ? displayName + "_" + accountName : id);
    }
}
