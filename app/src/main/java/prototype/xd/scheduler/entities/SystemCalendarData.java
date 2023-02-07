package prototype.xd.scheduler.entities;

import static prototype.xd.scheduler.utilities.QueryUtilities.getInt;
import static prototype.xd.scheduler.utilities.QueryUtilities.getLong;
import static prototype.xd.scheduler.utilities.QueryUtilities.getString;
import static prototype.xd.scheduler.utilities.Static.KEY_SEPARATOR;
import static prototype.xd.scheduler.utilities.SystemCalendarUtils.CALENDAR_COLUMNS;

import android.database.Cursor;
import android.provider.CalendarContract;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.Static;

public class SystemCalendarData {
    
    public static final String NAME = SystemCalendar.class.getSimpleName();
    
    @NonNull
    public final String accountType;
    @NonNull
    public final String accountName;
    
    @NonNull
    protected final String prefKey;
    @NonNull
    protected final List<String> subKeys;
    @NonNull
    protected final String visibilityKey;
    public final long id;
    public final int accessLevel;
    
    @NonNull
    public final String timeZoneId;
    @NonNull
    public final String displayName;
    @ColorInt
    public final int color;
    
    @NonNull
    public final List<SystemCalendarEventData> systemCalendarEventsData;
    
    protected final List<Long> exceptions;
    
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    SystemCalendarData(@NonNull Cursor cursor,
                       @NonNull List<SystemCalendarEventData> eventsData) {
        
        exceptions = new ArrayList<>();
        
        accountType = getString(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.ACCOUNT_TYPE);
        accountName = getString(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.ACCOUNT_NAME);
        displayName = getString(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
        id = getLong(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars._ID);
        accessLevel = getInt(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL);
        color = getInt(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.CALENDAR_COLOR);
        
        prefKey = accountName + KEY_SEPARATOR + displayName;
        subKeys = List.of(accountName, prefKey);
        visibilityKey = prefKey + KEY_SEPARATOR + Static.VISIBLE;
        
        String calTimeZoneId = getString(cursor, CALENDAR_COLUMNS, CalendarContract.Calendars.CALENDAR_TIME_ZONE);
        
        if (calTimeZoneId.isEmpty()) {
            Logger.warning(NAME, this + " has no timezone, defaulting to UTC");
            timeZoneId = "UTC";
        } else {
            timeZoneId = calTimeZoneId;
        }
        
        systemCalendarEventsData = eventsData;
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
        SystemCalendarData other = (SystemCalendarData) o;
        return id == other.id &&
                accessLevel == other.accessLevel &&
                color == other.color &&
                accountType.equals(other.accountType) &&
                prefKey.equals(other.prefKey) &&
                timeZoneId.equals(other.timeZoneId) &&
                systemCalendarEventsData.equals(other.systemCalendarEventsData);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(accountType, prefKey, id, accessLevel, timeZoneId, color, systemCalendarEventsData);
    }
    
    @NonNull
    @Override
    public String toString() {
        return NAME + ": " + (BuildConfig.DEBUG ? prefKey : id);
    }
}
