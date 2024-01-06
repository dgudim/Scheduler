package prototype.xd.scheduler.entities;

import static android.provider.CalendarContract.Events.STATUS_CANCELED;
import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_COLUMNS;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_EVENT_COLUMNS;
import static prototype.xd.scheduler.utilities.Utilities.remapMap;

import android.database.Cursor;
import android.provider.CalendarContract;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

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
    public final Map<Long, SystemCalendarEventData> systemCalendarEventsDataMap;
    
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
        
        systemCalendarEventsDataMap = new ArrayMap<>();
    }
    
    public void addEventData(@NonNull Cursor cursor) {
        SystemCalendarEventData data = new SystemCalendarEventData(cursor);
        systemCalendarEventsDataMap.put(data.id, data);
    }
    
    public void addExceptionToEvent(@NonNull Cursor cursor, long exception) {
        long originalId = getLong(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.ORIGINAL_ID);
        SystemCalendarEventData eventData = systemCalendarEventsDataMap.get(originalId);
        if (eventData == null) {
            Logger.warning(NAME, "Id " + originalId + " not found in " + this);
            return;
        }
        eventData.addException(exception);
        // not canceled means that this event is an overriding exception instead of deleting
        if (getInt(cursor, CALENDAR_EVENT_COLUMNS, CalendarContract.Events.STATUS) != STATUS_CANCELED) {
            addEventData(cursor);
        }
    }
    
    @NonNull
    public ArrayMap<Long, SystemCalendarEvent> makeEvents(@NonNull SystemCalendar calendar) {
        return remapMap(systemCalendarEventsDataMap, data -> new SystemCalendarEvent(data, calendar));
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
                Objects.equals(accountType, other.accountType) &&
                Objects.equals(accountName, other.accountName) &&
                Objects.equals(displayName, other.displayName) &&
                Objects.equals(timeZoneId, other.timeZoneId) &&
                systemCalendarEventsDataMap.equals(other.systemCalendarEventsDataMap);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(accountType, accountName, displayName, id, accessLevel, timeZoneId, color, systemCalendarEventsDataMap);
    }
    
    @NonNull
    @Override
    public String toString() {
        return NAME + ": " + (BuildConfig.DEBUG ? displayName + "_" + accountName : id);
    }
}
