package prototype.xd.scheduler.calendarUtilities;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Arrays;

public class SystemCalendarUtils {
    
    public static final ArrayList<String> calendarColumns = new ArrayList<>(Arrays.asList(
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CALENDAR_COLOR));
    
    public static final ArrayList<String> calendarEventsColumns = new ArrayList<>(Arrays.asList(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DISPLAY_COLOR,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DELETED));
    
    public static ArrayList<SystemCalendar> getAllCalendars(ContentResolver contentResolver) {
        ArrayList<SystemCalendar> systemCalendars = new ArrayList<>();
        Cursor cursor = query(contentResolver, CalendarContract.Calendars.CONTENT_URI, calendarColumns.toArray(new String[0]), null);
        int calendars = cursor.getCount();
        cursor.moveToFirst();
        for (int i = 0; i < calendars; i++) {
            systemCalendars.add(new SystemCalendar(cursor, contentResolver));
            cursor.moveToNext();
        }
        cursor.close();
        return systemCalendars;
    }
    
    public static ArrayList<String> generateSubKeysFromKey(String calendar_key){
        ArrayList<String> calendarSubKeys = new ArrayList<>();
        String[] key_split = calendar_key.split("_");
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < key_split.length; i++) {
            if (i != 0) {
                buffer.append('_');
            }
            buffer.append(key_split[i]);
            calendarSubKeys.add(buffer.toString());
        }
        return calendarSubKeys;
    }
    
    public static int getFirstValidKeyIndex(ArrayList<String> calendarSubKeys, String parameter) {
        for (int i = calendarSubKeys.size() - 1; i >= 0; i--) {
            try {
                if (preferences.getString(calendarSubKeys.get(i) + "_" + parameter, null) != null) {
                    return i;
                }
            } catch (ClassCastException e) {
                return i;
            }
        }
        return -1;
    }
    
    public static String getFirstValidKey(ArrayList<String> calendarSubKeys, String parameter) {
        int index = getFirstValidKeyIndex(calendarSubKeys, parameter);
        return index == -1 ? parameter : calendarSubKeys.get(index) + "_" + parameter;
    }
    
}
