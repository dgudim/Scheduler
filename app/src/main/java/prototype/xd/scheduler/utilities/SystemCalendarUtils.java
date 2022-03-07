package prototype.xd.scheduler.utilities;

import static android.util.Log.INFO;
import static prototype.xd.scheduler.utilities.Logger.log;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.QueryUtilities.query;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Arrays;

import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.entities.calendars.SystemCalendar;
import prototype.xd.scheduler.entities.calendars.SystemCalendarEvent;

public class SystemCalendarUtils {
    
    public static final ArrayList<String> calendarColumns = new ArrayList<>(Arrays.asList(
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_TIME_ZONE));
    
    public static final ArrayList<String> calendarEventsColumns = new ArrayList<>(Arrays.asList(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DISPLAY_COLOR,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DURATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.RDATE,
            CalendarContract.Events.EXRULE,
            CalendarContract.Events.EXDATE,
            CalendarContract.Events.DELETED,
            CalendarContract.Events.EVENT_TIMEZONE));
    
    public static ArrayList<SystemCalendar> getAllCalendars(Context context, boolean loadMinimal) {
        ContentResolver contentResolver = context.getContentResolver();
        
        ArrayList<SystemCalendar> systemCalendars = new ArrayList<>();
        Cursor cursor = query(contentResolver, CalendarContract.Calendars.CONTENT_URI, calendarColumns.toArray(new String[0]), null);
        int calendars = cursor.getCount();
        cursor.moveToFirst();
        for (int i = 0; i < calendars; i++) {
            systemCalendars.add(new SystemCalendar(cursor, contentResolver, loadMinimal));
            cursor.moveToNext();
        }
        cursor.close();
        return systemCalendars;
    }
    
    public static ArrayList<TodoListEntry> getTodoListEntriesFromCalendars(Context context, long day_start, long day_end) {
        ArrayList<TodoListEntry> todoListEntries = new ArrayList<>();
        ArrayList<SystemCalendar> calendars = getAllCalendars(context, false);
        for (int i = 0; i < calendars.size(); i++) {
            todoListEntries.addAll(calendars.get(i).getVisibleTodoListEntries(day_start, day_end));
        }
        log(INFO, "CalendarUtils", "read calendar entries: " + todoListEntries.size());
        return todoListEntries;
    }
    
    public static ArrayList<String> generateSubKeysFromKey(String calendar_key) {
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
    
    public static String makeKey(SystemCalendar calendar) {
        return calendar.account_name + "_" + calendar.name;
    }
    
    public static String makeKey(SystemCalendar calendar, int possibleEventColor) {
        return makeKey(calendar) + "_" + possibleEventColor;
    }
    
    public static String makeKey(SystemCalendarEvent event) {
        return makeKey(event.associatedCalendar, event.color);
    }
    
}
